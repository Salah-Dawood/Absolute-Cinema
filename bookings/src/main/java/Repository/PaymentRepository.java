package Repository;

import Enums.PaymentStatus;
import model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findFirstByBookingIdAndStatus(String bookingId, PaymentStatus status);
    List<Payment> findByBookingIdOrderByCreatedAtDesc(String bookingId);
}
