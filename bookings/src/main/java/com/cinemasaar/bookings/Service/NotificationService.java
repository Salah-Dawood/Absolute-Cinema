package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Enums.NotificationStatus;
import com.cinemasaar.bookings.Enums.NotificationType;
import com.cinemasaar.bookings.Model.BookedAddon;
import com.cinemasaar.bookings.Model.BookedSeat;
import com.cinemasaar.bookings.Model.Booking;
import com.cinemasaar.bookings.Model.Notification;
import com.cinemasaar.bookings.Model.Ticket;
import com.cinemasaar.bookings.Repository.NotificationRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final QRCodeService qrCodeService;

    public Notification sendTicketEmail(Booking booking, Ticket ticket, String email) {
        byte[] qrPng = qrCodeService.toPngBytes(ticket.getTicketCode());
        String subject = "Your CinemaSaar ticket - " + ticket.getTicketCode();
        return sendAndRecord(booking, NotificationType.BOOKING_CONFIRMED, email, subject,
                ticketHtml(booking, ticket), qrPng);
    }

    public Optional<Notification> sendCancellationEmail(Booking booking) {
        return notificationRepository
                .findFirstByBookingIdAndTypeAndStatusOrderByCreatedAtDesc(
                        booking.getId(), NotificationType.BOOKING_CONFIRMED, NotificationStatus.SENT)
                .map(previous -> sendAndRecord(booking, NotificationType.BOOKING_CANCELLED,
                        previous.getRecipientEmail(), "Your CinemaSaar booking was cancelled",
                        cancellationHtml(booking), null));
    }

    public List<Notification> getForBooking(String bookingId) {
        return notificationRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    private Notification sendAndRecord(Booking booking, NotificationType type, String email,
                                       String subject, String html, byte[] qrPng) {
        Notification notification = Notification.builder()
                .userId(booking.getUserId())
                .bookingId(booking.getId())
                .type(type)
                .recipientEmail(email)
                .subject(subject)
                .status(NotificationStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        try {
            emailService.send(email, subject, html, qrPng);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (MessagingException | MailException e) {
            log.warn("Email to {} failed: {}", email, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        return notificationRepository.save(notification);
    }

    private String ticketHtml(Booking booking, Ticket ticket) {
        StringBuilder seatRows = new StringBuilder();
        if (booking.getSeats() != null) {
            for (BookedSeat seat : booking.getSeats()) {
                StringBuilder addons = new StringBuilder();
                if (seat.getAddons() != null) {
                    for (BookedAddon addon : seat.getAddons()) {
                        if (addons.length() > 0) addons.append(", ");
                        addons.append(addon.getQuantity()).append(" x ").append(esc(addon.getName()));
                    }
                }
                seatRows.append("<tr>")
                        .append("<td style=\"padding:6px 12px;border-bottom:1px solid #eee\">")
                        .append(esc(seat.getSeatId())).append("</td>")
                        .append("<td style=\"padding:6px 12px;border-bottom:1px solid #eee\">")
                        .append(addons.length() == 0 ? "-" : addons).append("</td>")
                        .append("</tr>");
            }
        }

        return """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;color:#222">
                  <h2 style="color:#b91c1c;margin-bottom:4px">Your ticket is ready</h2>
                  <p style="margin-top:0">Show this QR code at the cinema entrance.</p>
                  <div style="text-align:center;margin:24px 0">
                    <img src="cid:ticketQr" alt="Ticket QR code" width="220" height="220">
                    <p style="font-size:20px;font-weight:bold;letter-spacing:2px">%s</p>
                  </div>
                  <table style="border-collapse:collapse;margin-bottom:16px">
                    <tr><td style="padding:4px 16px 4px 0"><b>Booking</b></td><td>%s</td></tr>
                    <tr><td style="padding:4px 16px 4px 0"><b>Showtime</b></td><td>%s</td></tr>
                    <tr><td style="padding:4px 16px 4px 0"><b>Total paid</b></td><td>%s</td></tr>
                  </table>
                  <table style="border-collapse:collapse">
                    <tr>
                      <th align="left" style="padding:6px 12px;border-bottom:2px solid #222">Seat</th>
                      <th align="left" style="padding:6px 12px;border-bottom:2px solid #222">Add-ons</th>
                    </tr>
                    %s
                  </table>
                  <p style="color:#888;font-size:12px;margin-top:24px">The QR code is also attached to this email.</p>
                </div>
                """.formatted(
                esc(ticket.getTicketCode()),
                esc(booking.getId()),
                esc(booking.getShowtimeId()),
                booking.getTotal(),
                seatRows);
    }

    private String cancellationHtml(Booking booking) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;color:#222">
                  <h2 style="color:#b91c1c">Your booking was cancelled</h2>
                  <p>Booking <b>%s</b> has been cancelled by the cinema.</p>
                  <p><b>Reason:</b> %s</p>
                  <p>Your ticket is no longer valid. A refund of <b>%s</b> will be processed.</p>
                </div>
                """.formatted(
                esc(booking.getId()),
                esc(booking.getCancellationReason()),
                booking.getTotal());
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}