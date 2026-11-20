package ma.youcode.covoiturage.dto.requests;

import jakarta.validation.constraints.*;

public record VehicleRequestDto(

        @NotBlank
        String make,

        @NotBlank
        String model,

        @NotBlank
        String registrationNumber,

        @NotBlank
        String color,

        @Min(1)
        @Max(8)
        int capacity,

        @NotNull
        Long driverId

) {}