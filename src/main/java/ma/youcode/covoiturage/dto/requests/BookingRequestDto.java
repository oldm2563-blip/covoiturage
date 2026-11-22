package ma.youcode.covoiturage.dto.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequestDto(
        @NotNull Long passengerId,
        @NotNull Long tripId,
        @NotNull Long boardingStopId,
        @NotNull Long alightingStopId,
        @Min(1) int numberOfSeats
) {}