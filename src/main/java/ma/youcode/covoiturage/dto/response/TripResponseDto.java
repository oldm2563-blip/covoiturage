package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.enums.Status;

import java.util.List;

public record TripResponseDto(
        Long id,
        Long vehicleId,
        DriverSummaryResponseDto driver,
        int seatsOffered,
        int availableSeats,
        Status status,
        List<StopResponseDto> stops
) {}