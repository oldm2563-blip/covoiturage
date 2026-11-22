package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.common.exception.BusinessRuleException;
import ma.youcode.covoiturage.common.exception.ForbiddenException;
import ma.youcode.covoiturage.common.exception.LicenseCodeAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.StopRequestDto;
import ma.youcode.covoiturage.dto.requests.TripRequestDto;
import ma.youcode.covoiturage.dto.requests.TripSearchRequestDto;
import ma.youcode.covoiturage.dto.response.TripResponseDto;
import ma.youcode.covoiturage.entities.*;
import ma.youcode.covoiturage.enums.BookingStatus;
import ma.youcode.covoiturage.enums.Status;
import ma.youcode.covoiturage.mapper.StopMapper;
import ma.youcode.covoiturage.mapper.TripMapper;
import ma.youcode.covoiturage.repositories.BookingRepository;
import ma.youcode.covoiturage.repositories.DriverRepository;
import ma.youcode.covoiturage.repositories.TripRepository;
import ma.youcode.covoiturage.repositories.VehicalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TripService {

    private final BookingRepository bookingRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final StopMapper stopMapper;
    private final VehicalRepository vehicalRepository;

    @Transactional
    public TripResponseDto createTrip(TripRequestDto dto){

        Driver driver = driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new ResourcesNotFoundException(
                        "Couldn't find a driver with this id: " + dto.driverId()
                ));


        Vehicle vehicle = vehicalRepository.findById(dto.vehicleId())
                .orElseThrow(() -> new ResourcesNotFoundException("Couldn't find a vehicle with this id :" + dto.vehicleId()));

        if (vehicle.getDriver().getId() != driver.getId()) {
            throw new ForbiddenException(
                    "You cannot publish a trip using another driver's vehicle"
            );
        }

        if (dto.stops().size() < 2){
            throw new BusinessRuleException("there should be a minimum of 2 Stops");
        }

        Set<String> cities = dto.stops()
                .stream()
                .map(StopRequestDto::city)
                .map(city -> city.trim().toLowerCase())
                .collect(Collectors.toSet());

        if (cities.size() != dto.stops().size()) {
            throw new BusinessRuleException("Stop cities must be unique");
        }
        if (dto.stops().get(0).price().compareTo(BigDecimal.ZERO) != 0){
            throw new BusinessRuleException("The first stop price must be 0");
        }

        for (int i = 1; i < dto.stops().size() ; i++){

            LocalDateTime previous = dto.stops().get(i-1).dateTime();
            LocalDateTime current = dto.stops().get(i).dateTime();

            if(!current.isAfter(previous)){
                throw new BusinessRuleException("Stop times must be in strictly increasing order");
            }
        }

        for (int i = 1; i < dto.stops().size() ; i++){

            BigDecimal previous = dto.stops().get(i-1).price();
            BigDecimal current = dto.stops().get(i).price();

            if(current.compareTo(previous) <= 0 ){
                throw new BusinessRuleException("Price should always continue to rise we dont like free rides");
            }
        }

        if (!dto.stops().get(0).dateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("The departure date should be in the future");
        }

        if (dto.seatsOffered() < 1){
            throw new BusinessRuleException("there should be at least 1 seat");
        }

        if (dto.seatsOffered() > vehicle.getCapacity()){
            throw new BusinessRuleException("you can't go over the capacity of the vehicle and they cant go in the trunk");
        }

        Trip trip = tripMapper.toEntity(dto);
        trip.setDriver(driver);
        trip.setVehicle(vehicle);

        List<Stop> stops = dto.stops()
                .stream()
                .map(stopMapper::toEntity)
                .toList();
        stops.forEach(stop -> stop.setTrip(trip));

        trip.setStops(stops);

        IntStream.range(0, stops.size())
                .forEach(i -> stops.get(i).setOrderIndex(i));

        Trip created = tripRepository.save(trip);

        return toResponseWithAvailableSeats(created);
    }

    public TripResponseDto getTripById(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourcesNotFoundException(
                        "Couldn't find a trip with this id: " + tripId
                ));

        return toResponseWithAvailableSeats(trip);
    }

    @Transactional
    public Page<TripResponseDto> searchTrips(TripSearchRequestDto dto, Pageable pageable) {
        LocalDateTime startOfDay = dto.date().atStartOfDay();

        LocalDateTime startOfNextDay = dto.date()
                .plusDays(1)
                .atStartOfDay();

        Pageable unsortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return tripRepository.searchTrips(
                dto.departureCity(),
                dto.arrivalCity(),
                dto.seatsRequired(),
                dto.maxPrice(),
                startOfDay,
                startOfNextDay,
                unsortedPageable
        ).map(this::toResponseWithAvailableSeats);
    }


    private TripResponseDto toResponseWithAvailableSeats(Trip trip) {

        Long occupiedSeats = bookingRepository.countOccupiedSeats(
                trip,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        int availableSeats = trip.getSeatsOffered() - occupiedSeats.intValue();

        TripResponseDto response = tripMapper.toResponse(trip);

        return new TripResponseDto(
                response.id(),
                response.vehicleId(),
                response.driver(),
                response.seatsOffered(),
                availableSeats,
                response.status(),
                response.stops()
        );
    }

    @Transactional
    public TripResponseDto cancelTrip(Long tripId, Long driverId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Trip not found"));

        // Only the owner can cancel the trip
        if (trip.getDriver().getId() != driverId) {
            throw new ForbiddenException(
                    "You can only cancel your own trips");
        }

        // Only published or full trips can be cancelled
        if (trip.getStatus() != Status.PUBLIE
                && trip.getStatus() != Status.COMPLET) {
            throw new BusinessRuleException(
                    "This trip cannot be cancelled");
        }

        // Retrieve all bookings belonging to this trip
        List<Booking> bookings = bookingRepository.findByTrip_Id(tripId);

        // Cancel all active bookings
        bookings.stream()
                .filter(booking ->
                        booking.getStatus() == BookingStatus.PENDING
                                || booking.getStatus() == BookingStatus.CONFIRMED)
                .forEach(booking ->
                        booking.setStatus(BookingStatus.CANCELLED));

        bookingRepository.saveAll(bookings);

        // Cancel the trip
        trip.setStatus(Status.ANNULE);
        Trip savedTrip = tripRepository.save(trip);

        return toResponseWithAvailableSeats(savedTrip);
    }

    @Transactional
    public TripResponseDto finishTrip(Long tripId, Long driverId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Trip not found"));

        // Only the owner can finish the trip
        if (trip.getDriver().getId() != driverId) {
            throw new ForbiddenException(
                    "You can only finish your own trips");
        }

        // Only active trips can be finished
        if (trip.getStatus() != Status.PUBLIE
                && trip.getStatus() != Status.COMPLET) {
            throw new BusinessRuleException(
                    "This trip cannot be finished");
        }

        // Departure must have already happened
        LocalDateTime departureTime = trip.getStops()
                .get(0)
                .getDateTime();

        if (LocalDateTime.now().isBefore(departureTime)) {
            throw new BusinessRuleException(
                    "You cannot finish a trip before its departure");
        }

        trip.setStatus(Status.TERMINE);
        Trip savedTrip = tripRepository.save(trip);

        return toResponseWithAvailableSeats(savedTrip);
    }
}
