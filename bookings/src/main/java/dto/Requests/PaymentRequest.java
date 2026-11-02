package dto.Requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(@NotBlank @Pattern(regexp = "\\d{12,19}", message = "must be 12 to 19 digits")
                             String cardNumber,
                             String idempotencyKey ) {
}
