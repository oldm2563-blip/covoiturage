package ma.youcode.covoiturage.dto.response;

public record VehicleResponseDto(
        Long id,
        String make,
        String model,
        String registrationNumber,
        String color,
        int capacity,
        Long driverId
) {}