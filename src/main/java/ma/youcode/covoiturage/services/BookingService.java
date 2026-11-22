package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.common.exception.BusinessRuleException;
import ma.youcode.covoiturage.common.exception.ForbiddenException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.BookingRequestDto;
import ma.youcode.covoiturage.dto.response.BookingResponseDto;
import ma.youcode.covoiturage.entities.*;
import ma.youcode.covoiturage.enums.BookingStatus;
import ma.youcode.covoiturage.enums.Status;
import ma.youcode.covoiturage.mapper.BookingMapper;
import ma.youcode.covoiturage.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final StopRepository stopRepository;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto dto) {

        // 1. Retrieve passenger and trip
        Passenger passenger = passengerRepository.findById(dto.passengerId())
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Passenger not found"));

        Trip trip = tripRepository.findById(dto.tripId())
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Trip not found"));

        // Check whether the passenger already has an active booking
        boolean alreadyBooked = bookingRepository
                .existsByPassenger_IdAndTrip_IdAndStatusIn(
                        dto.passengerId(),
                        dto.tripId(),
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                );

        if (alreadyBooked) {
            throw new BusinessRuleException(
                    "You already have an active booking for this trip"
            );
        }

        // 2. The trip must be published
        if (trip.getStatus() != Status.PUBLIE) {
            throw new BusinessRuleException(
                    "You cannot book a trip that is not published");
        }

        // 3. Retrieve boarding and alighting stops
        Stop boardingStop = stopRepository.findById(dto.boardingStopId())
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Boarding stop not found"));

        Stop alightingStop = stopRepository.findById(dto.alightingStopId())
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Alighting stop not found"));

        // 4. Both stops must belong to the selected trip
        if (boardingStop.getTrip().getId() != trip.getId()
                || alightingStop.getTrip().getId() != trip.getId()) {
            throw new BusinessRuleException(
                    "Both stops must belong to the selected trip");
        }

        // 5. Boarding must happen before alighting
        if (boardingStop.getOrderIndex() >= alightingStop.getOrderIndex()) {
            throw new BusinessRuleException(
                    "Boarding stop must come before alighting stop");
        }

        // 6. Calculate already occupied seats on the entire trip
        Long occupiedSeats = bookingRepository.countOccupiedSeats(
                trip,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (occupiedSeats == null) {
            occupiedSeats = 0L;
        }

        long availableSeats = trip.getSeatsOffered() - occupiedSeats;

// 7. Check seat availability BEFORE changing trip status
        if (dto.numberOfSeats() > availableSeats) {
            throw new BusinessRuleException(
                    "Not enough available seats on this trip. Available: "
                            + availableSeats
            );
        }

// 8. Calculate the segment price
        BigDecimal segmentPrice = alightingStop.getPrice()
                .subtract(boardingStop.getPrice());

        BigDecimal totalAmount = segmentPrice.multiply(
                BigDecimal.valueOf(dto.numberOfSeats())
        );

// 9. Create and save booking
        Booking booking = Booking.builder()
                .passenger(passenger)
                .trip(trip)
                .boardingStop(boardingStop)
                .alightingStop(alightingStop)
                .numberOfSeats(dto.numberOfSeats())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

// 10. Update trip status if no seats remain
        long remainingSeats = availableSeats - dto.numberOfSeats();

        if (remainingSeats == 0) {
            trip.setStatus(Status.COMPLET);
            tripRepository.save(trip);
        }

        return bookingMapper.toResponse(savedBooking);
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId, Long passengerId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Booking not found"));

        // Only the passenger who made the booking can cancel it
        if (booking.getPassenger().getId() != passengerId) {
            throw new ForbiddenException(
                    "You cannot cancel another passenger's booking");
        }

        LocalDateTime departureTime = booking.getTrip()
                .getStops()
                .get(0)
                .getDateTime();

        if (LocalDateTime.now().isAfter(departureTime.minusHours(24))) {
            throw new BusinessRuleException(
                    "Bookings can only be cancelled at least 24 hours before departure"
            );
        }

        // Only pending or confirmed bookings can be cancelled
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "This booking cannot be cancelled");
        }

        // Count occupied seats before cancelling the booking
        Long occupiedSeats = bookingRepository.countOccupiedSeats(
                booking.getTrip(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (occupiedSeats == null) {
            occupiedSeats = 0L;
        }

        // Cancel the booking
        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        // Calculate remaining seats after cancellation
        long remainingSeats = booking.getTrip().getSeatsOffered()
                - (occupiedSeats - booking.getNumberOfSeats());

        // Restore the trip status if seats are available
        if (remainingSeats > 0
                && booking.getTrip().getStatus() == Status.COMPLET) {

            booking.getTrip().setStatus(Status.PUBLIE);
            tripRepository.save(booking.getTrip());
        }

        return bookingMapper.toResponse(cancelledBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getTripBookings(Long tripId, Long driverId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Trip not found"));

        // Only the driver who owns this trip can view its bookings
        if (trip.getDriver().getId() != driverId) {
            throw new ForbiddenException(
                    "You can only view bookings for your own trips");
        }

        return bookingRepository.findByTrip_Id(tripId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getPassengerBookings(Long passengerId) {

        passengerRepository.findById(passengerId)
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Passenger not found"));

        return bookingRepository.findByPassenger_Id(passengerId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }
}