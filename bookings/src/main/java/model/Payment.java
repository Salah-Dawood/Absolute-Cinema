package model;


import Enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document("payments")
public class Payment {

    @Id
    private String id;
    private String bookingId;
    private String userId;
    private BigDecimal amount;
    private PaymentStatus status;

    @Indexed(unique = true)
    //same key sent twice = same payment, never charged twice
    private String idempotencyKey;

    private String cardLast4;
    private String failureReason;
    private Instant paidAt;
    private Instant createdAt;
}
