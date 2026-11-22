package ma.youcode.covoiturage.services;

import ma.youcode.covoiturage.common.exception.BusinessRuleException;
import ma.youcode.covoiturage.common.exception.ForbiddenException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.BookingRequestDto;
import ma.youcode.covoiturage.dto.response.BookingResponseDto;
import ma.youcode.covoiturage.entities.Booking;
import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.entities.Passenger;
import ma.youcode.covoiturage.entities.Stop;
import ma.youcode.covoiturage.entities.Trip;
import ma.youcode.covoiturage.enums.BookingStatus;
import ma.youcode.covoiturage.enums.Status;
import ma.youcode.covoiturage.mapper.BookingMapper;
import ma.youcode.covoiturage.repositories.BookingRepository;
import ma.youcode.covoiturage.repositories.PassengerRepository;
import ma.youcode.covoiturage.repositories.StopRepository;
import ma.youcode.covoiturage.repositories.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private StopRepository stopRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    // Shared fixture: trip 10 (owned by driver 5, 4 seats, published)
    // Nador (0 MAD) -> Taourirt (30 MAD) -> Oujda (50 MAD), departing in 3 days.
    private final List<BookingStatus> activeStatuses =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private Passenger passenger;
    private Trip trip;
    private Stop nadorStop;
    private Stop taourirtStop;
    private Stop oujdaStop;
    private BookingResponseDto expectedResponse;

    @BeforeEach
    void setUp() {
        Driver driver = new Driver();
        driver.setId(5L);

        passenger = new Passenger();
        passenger.setId(1L);

        trip = new Trip();
        trip.setId(10L);
        trip.setDriver(driver);
        trip.setSeatsOffered(4);
        trip.setStatus(Status.PUBLIE);

        nadorStop = buildStop(100L, 0, "Nador", "0");
        taourirtStop = buildStop(101L, 1, "Taourirt", "30");
        oujdaStop = buildStop(102L, 2, "Oujda", "50");
        trip.setStops(List.of(nadorStop, taourirtStop, oujdaStop));

        expectedResponse = new BookingResponseDto(
                500L, 1L, 10L, "Taourirt", "Oujda", 2,
                new BigDecimal("40"), LocalDateTime.now(), BookingStatus.PENDING);
    }

    // ---------------- CREATE BOOKING: SUCCESS ----------------

    @Test
    void shouldCreatePendingBookingWhenRequestIsValid() {
        // Arrange
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 100L, 102L, 2);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(100L)).thenReturn(Optional.of(nadorStop));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(oujdaStop));
        when(bookingRepository.countOccupiedSeats(trip, activeStatuses)).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(expectedResponse);

        // Act
        BookingResponseDto result = bookingService.createBooking(request);

        // Assert
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(saved.getValue().getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getValue().getPassenger()).isSameAs(passenger);
        assertThat(saved.getValue().getTrip()).isSameAs(trip);
        assertThat(saved.getValue().getNumberOfSeats()).isEqualTo(2);

        // 1 seat still free -> trip stays published and is not saved again
        assertThat(trip.getStatus()).isEqualTo(Status.PUBLIE);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldCalculatePriceAsStopPriceDifferenceTimesNumberOfSeats() {
        // Arrange: Taourirt (30) -> Oujda (50) for 2 seats = (50 - 30) * 2 = 40
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 101L, 102L, 2);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(101L)).thenReturn(Optional.of(taourirtStop));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(oujdaStop));
        when(bookingRepository.countOccupiedSeats(trip, activeStatuses)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(expectedResponse);

        // Act
        bookingService.createBooking(request);

        // Assert
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("40");
    }

    @Test
    void shouldMarkTripAsFullWhenLastSeatsAreBooked() {
        // Arrange: 4 seats offered, 2 already taken, this booking takes the last 2
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 100L, 102L, 2);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(100L)).thenReturn(Optional.of(nadorStop));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(oujdaStop));
        when(bookingRepository.countOccupiedSeats(trip, activeStatuses)).thenReturn(2L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(expectedResponse);

        // Act
        bookingService.createBooking(request);

        // Assert
        assertThat(trip.getStatus()).isEqualTo(Status.COMPLET);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldAllowBookingAgainWhenThePreviousBookingWasCancelled() {
        // Arrange: only PENDING/CONFIRMED bookings count as "active",
        // so a CANCELLED one makes this query return false.
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 100L, 102L, 1);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByPassenger_IdAndTrip_IdAndStatusIn(1L, 10L, activeStatuses))
                .thenReturn(false);
        when(stopRepository.findById(100L)).thenReturn(Optional.of(nadorStop));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(oujdaStop));
        when(bookingRepository.countOccupiedSeats(trip, activeStatuses)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(expectedResponse);

        // Act
        BookingResponseDto result = bookingService.createBooking(request);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(bookingRepository).save(any(Booking.class));
    }

    // ---------------- CREATE BOOKING: FAILURES ----------------

    @Test
    void shouldRejectBookingWhenPassengerDoesNotExist() {
        // Arrange
        BookingRequestDto request = new BookingRequestDto(99L, 10L, 100L, 102L, 1);
        when(passengerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectBookingWhenTripDoesNotExist() {
        // Arrange
        BookingRequestDto request = new BookingRequestDto(1L, 99L, 100L, 102L, 1);
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectBookingWhenTripIsNotPublished() {
        // Arrange
        trip.setStatus(Status.COMPLET);
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 100L, 102L, 1);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        // Act + Assert
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not published");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectBookingWhenPassengerAlreadyHasAnActiveBookingOnTheTrip() {
        // Arrange
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 100L, 102L, 1);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByPassenger_IdAndTrip_IdAndStatusIn(1L, 10L, activeStatuses))
                .thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already have an active booking");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectBookingWhenAStopBelongsToAnotherTrip() {
        // Arrange
        Trip otherTrip = new Trip();
        otherTrip.setId(99L);
        Stop foreignStop = buildStop(200L, 1, "Casablanca", "40");
        foreignStop.setTrip(otherTrip);

        BookingRequestDto request = new BookingRequestDto(1L, 10L, 100L, 200L, 1);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(100L)).thenReturn(Optional.of(nadorStop));
        when(stopRepository.findById(200L)).thenReturn(Optional.of(foreignStop));

        // Act + Assert
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must belong to the selected trip");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectBookingWhenBoardingStopComesAfterAlightingStop() {
        // Arrange: boarding at Oujda (index 2), alighting at Nador (index 0)
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 102L, 100L, 1);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(oujdaStop));
        when(stopRepository.findById(100L)).thenReturn(Optional.of(nadorStop));

        // Act + Assert
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must come before");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectBookingWhenNotEnoughSeatsAreAvailable() {
        // Arrange: 4 seats offered, 3 occupied -> only 1 left, but 2 requested
        BookingRequestDto request = new BookingRequestDto(1L, 10L, 100L, 102L, 2);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(100L)).thenReturn(Optional.of(nadorStop));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(oujdaStop));
        when(bookingRepository.countOccupiedSeats(trip, activeStatuses)).thenReturn(3L);

        // Act + Assert
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Not enough available seats");

        verify(bookingRepository, never()).save(any());
    }

    // ---------------- CANCEL BOOKING ----------------

    @Test
    void shouldCancelBookingWhenOwnerCancelsMoreThan24HoursBeforeDeparture() {
        // Arrange
        Booking booking = buildBooking(BookingStatus.PENDING, 2);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(bookingRepository.countOccupiedSeats(trip, activeStatuses)).thenReturn(2L);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(expectedResponse);

        // Act
        BookingResponseDto result = bookingService.cancelBooking(500L, 1L);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);

        // trip was not full, so its status does not change
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldRejectCancellationByAnotherPassenger() {
        // Arrange
        Booking booking = buildBooking(BookingStatus.PENDING, 2); // belongs to passenger 1
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        // Act + Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(500L, 2L))
                .isInstanceOf(ForbiddenException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectCancellationLessThan24HoursBeforeDeparture() {
        // Arrange: departure in 10 hours
        nadorStop.setDateTime(LocalDateTime.now().plusHours(10));
        Booking booking = buildBooking(BookingStatus.CONFIRMED, 2);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        // Act + Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(500L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("24 hours");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectCancellationOfAnAlreadyCancelledBooking() {
        // Arrange
        Booking booking = buildBooking(BookingStatus.CANCELLED, 2);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        // Act + Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(500L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be cancelled");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRestoreTripToPublishedWhenCancellationFreesSeatsOnAFullTrip() {
        // Arrange: trip is full (4/4 seats taken); cancelling 2 seats frees some
        trip.setStatus(Status.COMPLET);
        Booking booking = buildBooking(BookingStatus.CONFIRMED, 2);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(bookingRepository.countOccupiedSeats(trip, activeStatuses)).thenReturn(4L);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(expectedResponse);

        // Act
        bookingService.cancelBooking(500L, 1L);

        // Assert
        assertThat(trip.getStatus()).isEqualTo(Status.PUBLIE);
        verify(tripRepository).save(trip);
    }

    // ---------------- RETRIEVE BOOKINGS ----------------

    @Test
    void shouldReturnBookingsOfATripToItsOwnerDriver() {
        // Arrange
        Booking first = buildBooking(BookingStatus.PENDING, 1);
        Booking second = buildBooking(BookingStatus.CONFIRMED, 2);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTrip_Id(10L)).thenReturn(List.of(first, second));
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(expectedResponse);

        // Act
        List<BookingResponseDto> result = bookingService.getTripBookings(10L, 5L);

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldRejectAccessToBookingsOfAnotherDriversTrip() {
        // Arrange: trip belongs to driver 5, driver 6 asks for its bookings
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        // Act + Assert
        assertThatThrownBy(() -> bookingService.getTripBookings(10L, 6L))
                .isInstanceOf(ForbiddenException.class);

        verify(bookingRepository, never()).findByTrip_Id(any());
    }

    @Test
    void shouldReturnBookingHistoryOfAPassenger() {
        // Arrange
        Booking first = buildBooking(BookingStatus.COMPLETED, 1);
        Booking second = buildBooking(BookingStatus.CANCELLED, 2);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByPassenger_Id(1L)).thenReturn(List.of(first, second));
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(expectedResponse);

        // Act
        List<BookingResponseDto> result = bookingService.getPassengerBookings(1L);

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldRejectBookingHistoryWhenPassengerDoesNotExist() {
        // Arrange
        when(passengerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> bookingService.getPassengerBookings(99L))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(bookingRepository, never()).findByPassenger_Id(any());
    }

    // ---------------- TEST HELPERS ----------------

    private Stop buildStop(long id, int orderIndex, String city, String price) {
        Stop stop = new Stop();
        stop.setId(id);
        stop.setTrip(trip);
        stop.setOrderIndex(orderIndex);
        stop.setCity(city);
        stop.setPrice(new BigDecimal(price));
        stop.setDateTime(LocalDateTime.now().plusDays(3).plusHours(orderIndex));
        return stop;
    }

    private Booking buildBooking(BookingStatus status, int numberOfSeats) {
        return Booking.builder()
                .id(500L)
                .passenger(passenger)
                .trip(trip)
                .boardingStop(taourirtStop)
                .alightingStop(oujdaStop)
                .numberOfSeats(numberOfSeats)
                .status(status)
                .build();
    }
}