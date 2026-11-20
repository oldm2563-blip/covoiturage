package ma.youcode.covoiturage.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TripRequestDto(
        @NotNull Long vehicleId,
        @Min(1) int seatsOffered,
        @NotEmpty List<@Valid StopRequestDto> stops
) {}