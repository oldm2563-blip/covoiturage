package ma.youcode.covoiturage.dto.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripSearchRequestDto(
        @NotBlank String departureCity,
        @NotBlank String arrivalCity,
        @NotNull LocalDate date,
        @Min(1) int seatsRequired,
        BigDecimal maxPrice
) {}
