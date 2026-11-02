package com.cinemasaar.queue.reservation.service;

import com.cinemasaar.queue.admission.model.QueueEntry;
import com.cinemasaar.queue.admission.service.AdmissionService;
import com.cinemasaar.queue.common.exception.NotFoundException;
import com.cinemasaar.queue.config.BookingProperties;
import com.cinemasaar.queue.reservation.dto.AddonSelection;
import com.cinemasaar.queue.reservation.exception.CartExpiredException;
import com.cinemasaar.queue.reservation.exception.CartStateException;
import com.cinemasaar.queue.reservation.exception.SeatUnavailableException;
import com.cinemasaar.queue.reservation.model.Addon;
import com.cinemasaar.queue.reservation.model.AddonLine;
import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartItem;
import com.cinemasaar.queue.reservation.model.CartStatus;
import com.cinemasaar.queue.reservation.model.SeatStatus;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import com.cinemasaar.queue.reservation.repository.AddonRepository;
import com.cinemasaar.queue.reservation.repository.BookingCartRepository;
import com.cinemasaar.queue.reservation.repository.ShowtimeSeatRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final MongoTemplate mongo;
    private final BookingCartRepository carts;
    private final ShowtimeSeatRepository seats;
    private final AddonRepository addons;
    private final AdmissionService admission;
    private final BookingProperties props;

    public CartService(MongoTemplate mongo,
                       BookingCartRepository carts,
                       ShowtimeSeatRepository seats,
                       AddonRepository addons,
                       AdmissionService admission,
                       BookingProperties props) {
        this.mongo = mongo;
        this.carts = carts;
        this.seats = seats;
        this.addons = addons;
        this.admission = admission;
        this.props = props;
    }

    @Transactional
    public BookingCart createCart(String userId, String admissionToken) {
        QueueEntry entry = admission.requireAdmission(admissionToken, userId);
        Instant now = Instant.now();

        BookingCart cart = BookingCart.open(
                UUID.randomUUID().toString(),
                entry.getId(),
                userId,
                entry.getShowtimeId(),
                now,
                now.plus(props.cartTtl()));

        carts.insert(cart);
        admission.attachCart(entry.getId(), cart.getId(), cart.getExpiresAt());
        return cart;
    }

    @Transactional
    public BookingCart addSeat(String cartId, String userId, String seatId) {
        BookingCart cart = requireOpenCart(cartId, userId);

        if (cart.getItems().size() >= props.maxSeatsPerCart()) {
            throw new CartStateException(
                    "A cart may hold at most " + props.maxSeatsPerCart() + " seats");
        }
        if (cart.hasSeat(seatId)) {
            return cart;
        }

        ShowtimeSeat seat = seats.findByShowtimeIdAndSeatId(cart.getShowtimeId(), seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found: " + seatId));

        Instant now = Instant.now();

        Query q = Query.query(new Criteria().andOperator(
                Criteria.where("showtimeId").is(cart.getShowtimeId()),
                Criteria.where("seatId").is(seatId),
                new Criteria().orOperator(
                        Criteria.where("status").is(SeatStatus.AVAILABLE),
                        Criteria.where("status").is(SeatStatus.HELD)
                                .and("holdExpiresAt").lte(now))));

        Update u = new Update()
                .set("status", SeatStatus.HELD)
                .set("cartId", cart.getId())
                .set("heldBy", userId)
                .set("holdExpiresAt", cart.getExpiresAt())
                .set("updatedAt", now);

        if (mongo.updateFirst(q, u, ShowtimeSeat.class).getModifiedCount() != 1) {
            throw new SeatUnavailableException(seatId);
        }

        cart.addItem(seat);
        return carts.save(cart);
    }

    @Transactional
    public BookingCart removeSeat(String cartId, String userId, String seatId) {
        BookingCart cart = requireOpenCart(cartId, userId);
        CartItem removed = cart.removeItem(seatId);
        if (removed == null) {
            return cart;
        }

        mongo.updateFirst(
                Query.query(Criteria.where("showtimeId").is(cart.getShowtimeId())
                        .and("seatId").is(seatId)
                        .and("cartId").is(cartId)
                        .and("status").is(SeatStatus.HELD)),
                new Update()
                        .set("status", SeatStatus.AVAILABLE)
                        .unset("cartId")
                        .unset("heldBy")
                        .unset("holdExpiresAt")
                        .set("updatedAt", Instant.now()),
                ShowtimeSeat.class);

        return carts.save(cart);
    }

    @Transactional
    public BookingCart setAddons(String cartId, String userId, String seatId,
                                 List<AddonSelection> selections) {
        BookingCart cart = requireOpenCart(cartId, userId);
        CartItem item = cart.item(seatId)
                .orElseThrow(() -> new NotFoundException("Seat is not in this cart: " + seatId));

        ShowtimeSeat seat = seats.findByShowtimeIdAndSeatId(cart.getShowtimeId(), seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found: " + seatId));
        if (seat.getStatus() != SeatStatus.HELD || !cartId.equals(seat.getCartId())) {
            throw new CartExpiredException();
        }

        List<String> ids = selections.stream()
                .map(AddonSelection::addonId)
                .distinct()
                .toList();

        List<Addon> found = new ArrayList<>();
        addons.findAllById(ids).forEach(found::add);

        Map<String, Addon> catalog = found.stream()
                .collect(Collectors.toMap(Addon::getId, Function.identity()));

        if (catalog.size() != ids.size()) {
            throw new NotFoundException("Unknown add-on in selection");
        }

        List<AddonLine> lines = selections.stream().map(selection -> {
            if (selection.quantity() > props.maxAddonsPerSeat()) {
                throw new CartStateException(
                        "At most " + props.maxAddonsPerSeat() + " of one add-on per seat");
            }
            Addon addon = catalog.get(selection.addonId());
            if (!addon.isActive()) {
                throw new CartStateException("Add-on is not available: " + addon.getId());
            }
            return new AddonLine(
                    addon.getId(),
                    addon.getName(),
                    selection.quantity(),
                    addon.getPrice());
        }).toList();

        item.setAddons(lines);
        cart.recalculate();
        return carts.save(cart);
    }

    public BookingCart getCart(String cartId, String userId) {
        return carts.findByIdAndUserId(cartId, userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));
    }

    public BookingCart requireOpenCart(String cartId, String userId) {
        BookingCart cart = getCart(cartId, userId);
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new CartStateException("Cart is not open: " + cart.getStatus());
        }
        if (!cart.getExpiresAt().isAfter(Instant.now())) {
            throw new CartExpiredException();
        }
        return cart;
    }
}
