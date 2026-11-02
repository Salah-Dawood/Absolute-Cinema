package Repository;

import Enums.BookingStatus;
import model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    boolean existsBySeatHoldIdAndStatusIn(String seatHoldId, Collection<BookingStatus> statuses);
}
