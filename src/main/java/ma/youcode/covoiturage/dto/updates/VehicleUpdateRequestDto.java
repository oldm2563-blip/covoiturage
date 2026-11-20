package ma.youcode.covoiturage.dto.updates;

import jakarta.validation.constraints.*;

public record VehicleUpdateRequestDto(

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
        Integer capacity

) {}