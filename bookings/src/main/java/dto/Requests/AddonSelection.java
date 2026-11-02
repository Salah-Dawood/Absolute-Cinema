package dto.Requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddonSelection(@NotBlank String addonId, @Min(1) @Max(10) int quantity) {
}
