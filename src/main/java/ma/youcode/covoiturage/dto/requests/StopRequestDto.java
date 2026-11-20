package ma.youcode.covoiturage.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StopRequestDto(
        @NotBlank String city,
        @NotNull LocalDateTime dateTime,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {}