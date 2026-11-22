package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponseDto(
        Long id,
        Long passengerId,
        Long tripId,
        String boardingCity,
        String alightingCity,
        int numberOfSeats,
        BigDecimal totalAmount,
        LocalDateTime bookingDate,
        BookingStatus status
) {}