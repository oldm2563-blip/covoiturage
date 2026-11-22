package ma.youcode.covoiturage.services;

import ma.youcode.covoiturage.common.exception.BusinessRuleException;
import ma.youcode.covoiturage.common.exception.ForbiddenException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.StopRequestDto;
import ma.youcode.covoiturage.dto.requests.TripRequestDto;
import ma.youcode.covoiturage.dto.requests.TripSearchRequestDto;
import ma.youcode.covoiturage.dto.response.TripResponseDto;
import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.entities.Booking;
import ma.youcode.covoiturage.entities.Stop;
import ma.youcode.covoiturage.entities.Trip;
import ma.youcode.covoiturage.entities.Vehicle;
import ma.youcode.covoiturage.enums.Status;
import ma.youcode.covoiturage.enums.BookingStatus;
import ma.youcode.covoiturage.mapper.StopMapper;
import ma.youcode.covoiturage.mapper.TripMapper;
import ma.youcode.covoiturage.repositories.BookingRepository;
import ma.youcode.covoiturage.repositories.DriverRepository;
import ma.youcode.covoiturage.repositories.TripRepository;
import ma.youcode.covoiturage.repositories.VehicalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMapper tripMapper;

    @Mock
    private StopMapper stopMapper;

    @Mock
    private VehicalRepository vehicalRepository;

    @InjectMocks
    private TripService tripService;


    // ---------------- SUCCESSFUL CREATION ----------------

    @Test
    void shouldCreateTripSuccessfully() {

        Driver driver = createDriver();
        Vehicle vehicle = createVehicle(4, driver);
        TripRequestDto dto = validRequest();

        Trip trip = new Trip();

        TripResponseDto expectedResponse = new TripResponseDto(
                1L,
                1L,
                null,
                2,
                2,
                Status.PUBLIE,
                List.of()
        );

        when(tripMapper.toResponse(trip))
                .thenReturn(expectedResponse);

        Stop stop1 = new Stop();
        Stop stop2 = new Stop();

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(driver));

        when(vehicalRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        when(tripMapper.toEntity(dto))
                .thenReturn(trip);

        when(stopMapper.toEntity(dto.stops().get(0)))
                .thenReturn(stop1);

        when(stopMapper.toEntity(dto.stops().get(1)))
                .thenReturn(stop2);

        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // A newly created trip has no occupied seats.
        when(bookingRepository.countOccupiedSeats(
                any(Trip.class),
                anyCollection()
        )).thenReturn(0L);

        tripService.createTrip(dto);

        assertThat(trip.getDriver()).isEqualTo(driver);
        assertThat(trip.getVehicle()).isEqualTo(vehicle);

        assertThat(trip.getStops())
                .containsExactly(stop1, stop2);

        assertThat(stop1.getTrip()).isEqualTo(trip);
        assertThat(stop2.getTrip()).isEqualTo(trip);

        assertThat(stop1.getOrderIndex()).isZero();
        assertThat(stop2.getOrderIndex()).isEqualTo(1);

        verify(tripRepository, times(1)).save(trip);
        verify(tripMapper).toResponse(trip);
        verify(bookingRepository).countOccupiedSeats(
                trip,
                List.of(
                        ma.youcode.covoiturage.enums.BookingStatus.PENDING,
                        ma.youcode.covoiturage.enums.BookingStatus.CONFIRMED
                )
        );
    }


    // ---------------- DRIVER / VEHICLE VALIDATIONS ----------------

    @Test
    void shouldThrowExceptionWhenDriverDoesNotExist() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.createTrip(validRequest()))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(vehicalRepository, never()).findById(any());
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenVehicleDoesNotExist() {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(createDriver()));

        when(vehicalRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.createTrip(validRequest()))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldRejectTripWhenVehicleBelongsToAnotherDriver() {

        Driver requestedDriver = createDriver();

        Driver vehicleOwner = new Driver();
        vehicleOwner.setId(2L);

        Vehicle vehicle = createVehicle(4, vehicleOwner);

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(requestedDriver));

        when(vehicalRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> tripService.createTrip(validRequest()))
                .isInstanceOf(ForbiddenException.class);

        verify(tripRepository, never()).save(any());
    }


    // ---------------- STOP VALIDATIONS ----------------

    @Test
    void shouldRejectTripWithLessThanTwoStops() {

        TripRequestDto dto = requestWithStops(
                List.of(stop("Nador", 1, 0))
        );

        assertBusinessRule(dto);
    }

    @Test
    void shouldRejectDuplicateCities() {

        TripRequestDto dto = requestWithStops(
                List.of(
                        stop("Nador", 1, 0),
                        stop("nador", 2, 20)
                )
        );

        assertBusinessRule(dto);
    }

    @Test
    void shouldRejectCitiesWithWhitespaceDuplicates() {

        TripRequestDto dto = requestWithStops(
                List.of(
                        stop("Nador", 1, 0),
                        stop(" Nador ", 2, 20)
                )
        );

        assertBusinessRule(dto);
    }

    @Test
    void shouldRejectNonZeroFirstStopPrice() {

        TripRequestDto dto = requestWithStops(
                List.of(
                        stop("Nador", 1, 10),
                        stop("Oujda", 2, 20)
                )
        );

        assertBusinessRule(dto);
    }

    @Test
    void shouldRejectNonIncreasingStopTimes() {

        LocalDateTime departure = LocalDateTime.now().plusDays(1);

        TripRequestDto dto = new TripRequestDto(
                1L,
                1L,
                2,
                List.of(
                        new StopRequestDto("Nador", departure, BigDecimal.ZERO),
                        new StopRequestDto("Oujda", departure, BigDecimal.valueOf(20))
                )
        );

        assertBusinessRule(dto);
    }

    @Test
    void shouldRejectNonIncreasingPrices() {

        TripRequestDto dto = requestWithStops(
                List.of(
                        stop("Nador", 1, 0),
                        stop("Oujda", 2, 0)
                )
        );

        assertBusinessRule(dto);
    }

    @Test
    void shouldRejectPastDepartureDate() {

        LocalDateTime past = LocalDateTime.now().minusDays(1);

        TripRequestDto dto = new TripRequestDto(
                1L,
                1L,
                2,
                List.of(
                        new StopRequestDto("Nador", past, BigDecimal.ZERO),
                        new StopRequestDto("Oujda", past.plusHours(1), BigDecimal.valueOf(20))
                )
        );

        assertBusinessRule(dto);
    }


    // ---------------- SEAT VALIDATIONS ----------------

    @Test
    void shouldRejectZeroSeats() {

        TripRequestDto dto = new TripRequestDto(
                1L,
                1L,
                0,
                validStops()
        );

        assertBusinessRule(dto);
    }

    @Test
    void shouldRejectSeatsAboveVehicleCapacity() {

        TripRequestDto dto = new TripRequestDto(
                1L,
                1L,
                5,
                validStops()
        );

        assertBusinessRule(dto);
    }


    // ---------------- RETRIEVE / SEARCH ----------------

    @Test
    void shouldReturnTripWithAvailableSeatsWhenTripExists() {
        // Arrange
        Trip trip = buildTrip(Status.PUBLIE);
        trip.setSeatsOffered(4);
        TripResponseDto mapped = new TripResponseDto(
                10L, 1L, null, 4, 0, Status.PUBLIE, List.of());

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.countOccupiedSeats(
                trip, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(1L);
        when(tripMapper.toResponse(trip)).thenReturn(mapped);

        // Act
        TripResponseDto result = tripService.getTripById(10L);

        // Assert: 4 seats offered - 1 occupied = 3 available
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.availableSeats()).isEqualTo(3);
    }

    @Test
    void shouldThrowWhenTripIsNotFound() {
        // Arrange
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> tripService.getTripById(99L))
                .isInstanceOf(ResourcesNotFoundException.class);
    }

    @Test
    void shouldSearchTripsOverTheWholeDayAndIgnoreClientSorting() {
        // Arrange
        TripSearchRequestDto request = new TripSearchRequestDto(
                "Nador", "Oujda", LocalDate.of(2026, 12, 1), 2, BigDecimal.valueOf(50));
        Pageable clientPageable = PageRequest.of(0, 10, Sort.by("id"));
        Pageable expectedPageable = PageRequest.of(0, 10); // sorting removed by the service

        Trip trip = buildTrip(Status.PUBLIE);
        trip.setSeatsOffered(4);
        TripResponseDto mapped = new TripResponseDto(
                10L, 1L, null, 4, 0, Status.PUBLIE, List.of());

        when(tripRepository.searchTrips(
                "Nador",
                "Oujda",
                2,
                BigDecimal.valueOf(50),
                LocalDateTime.of(2026, 12, 1, 0, 0),
                LocalDateTime.of(2026, 12, 2, 0, 0),
                expectedPageable))
                .thenReturn(new PageImpl<>(List.of(trip)));
        when(bookingRepository.countOccupiedSeats(any(Trip.class), anyCollection()))
                .thenReturn(0L);
        when(tripMapper.toResponse(trip)).thenReturn(mapped);

        // Act
        Page<TripResponseDto> result = tripService.searchTrips(request, clientPageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).availableSeats()).isEqualTo(4);
    }


    // ---------------- CANCEL TRIP ----------------

    @Test
    void shouldCancelTripAndItsActiveBookings() {
        // Arrange
        Trip trip = buildTrip(Status.PUBLIE);
        trip.setSeatsOffered(4);

        Booking pending = Booking.builder().status(BookingStatus.PENDING).build();
        Booking confirmed = Booking.builder().status(BookingStatus.CONFIRMED).build();
        Booking alreadyCancelled = Booking.builder().status(BookingStatus.CANCELLED).build();
        List<Booking> bookings = List.of(pending, confirmed, alreadyCancelled);

        TripResponseDto mapped = new TripResponseDto(
                10L, 1L, null, 4, 4, Status.ANNULE, List.of());

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTrip_Id(10L)).thenReturn(bookings);
        when(tripRepository.save(trip)).thenReturn(trip);
        when(bookingRepository.countOccupiedSeats(any(Trip.class), anyCollection()))
                .thenReturn(0L);
        when(tripMapper.toResponse(trip)).thenReturn(mapped);

        // Act
        tripService.cancelTrip(10L, 1L);

        // Assert
        assertThat(trip.getStatus()).isEqualTo(Status.ANNULE);
        assertThat(pending.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(alreadyCancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).saveAll(bookings);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldRejectCancellationByAnotherDriver() {
        // Arrange
        Trip trip = buildTrip(Status.PUBLIE); // owned by driver 1
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        // Act + Assert
        assertThatThrownBy(() -> tripService.cancelTrip(10L, 2L))
                .isInstanceOf(ForbiddenException.class);

        assertThat(trip.getStatus()).isEqualTo(Status.PUBLIE);
        verify(tripRepository, never()).save(any());
    }


    // ---------------- FINISH TRIP ----------------

    @Test
    void shouldFinishTripAfterItsDeparture() {
        // Arrange
        Trip trip = buildTrip(Status.PUBLIE);
        trip.setSeatsOffered(4);

        Stop departure = new Stop();
        departure.setDateTime(LocalDateTime.now().minusHours(1));
        trip.setStops(List.of(departure));

        TripResponseDto mapped = new TripResponseDto(
                10L, 1L, null, 4, 4, Status.TERMINE, List.of());

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);
        when(bookingRepository.countOccupiedSeats(any(Trip.class), anyCollection()))
                .thenReturn(0L);
        when(tripMapper.toResponse(trip)).thenReturn(mapped);

        // Act
        tripService.finishTrip(10L, 1L);

        // Assert
        assertThat(trip.getStatus()).isEqualTo(Status.TERMINE);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldRejectFinishingTripBeforeItsDeparture() {
        // Arrange
        Trip trip = buildTrip(Status.PUBLIE);

        Stop departure = new Stop();
        departure.setDateTime(LocalDateTime.now().plusHours(2));
        trip.setStops(List.of(departure));

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        // Act + Assert
        assertThatThrownBy(() -> tripService.finishTrip(10L, 1L))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(trip.getStatus()).isEqualTo(Status.PUBLIE);
        verify(tripRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"TERMINE", "ANNULE"})
    void shouldRejectFinishingTripThatIsAlreadyCompletedOrCancelled(Status finalStatus) {
        // Arrange
        Trip trip = buildTrip(finalStatus);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        // Act + Assert
        assertThatThrownBy(() -> tripService.finishTrip(10L, 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(tripRepository, never()).save(any());
    }

    // ---------------- TEST HELPERS ----------------

    private void assertBusinessRule(TripRequestDto dto) {

        when(driverRepository.findById(1L))
                .thenReturn(Optional.of(createDriver()));

        when(vehicalRepository.findById(1L))
                .thenReturn(Optional.of(createVehicle(4, createDriver())));

        assertThatThrownBy(() -> tripService.createTrip(dto))
                .isInstanceOf(BusinessRuleException.class);

        verify(tripRepository, never()).save(any());
    }

    private Driver createDriver() {

        Driver driver = new Driver();
        driver.setId(1L);

        return driver;
    }

    private Vehicle createVehicle(int capacity, Driver driver) {

        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setCapacity(capacity);
        vehicle.setDriver(driver);

        return vehicle;
    }

    private Trip buildTrip(Status status) {

        Trip trip = new Trip();
        trip.setId(10L);
        trip.setDriver(createDriver());
        trip.setStatus(status);

        return trip;
    }

    private TripRequestDto validRequest() {

        return new TripRequestDto(
                1L,
                1L,
                2,
                validStops()
        );
    }

    private List<StopRequestDto> validStops() {

        return List.of(
                stop("Nador", 1, 0),
                stop("Oujda", 2, 20)
        );
    }

    private TripRequestDto requestWithStops(List<StopRequestDto> stops) {

        return new TripRequestDto(1L, 1L, 2, stops);
    }

    private StopRequestDto stop(String city, int hours, int price) {

        return new StopRequestDto(
                city,
                LocalDateTime.now().plusDays(1).plusHours(hours),
                BigDecimal.valueOf(price)
        );
    }
}