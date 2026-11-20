package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.common.exception.BusinessRuleException;
import ma.youcode.covoiturage.common.exception.LicenseCodeAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.StopRequestDto;
import ma.youcode.covoiturage.dto.requests.TripRequestDto;
import ma.youcode.covoiturage.dto.response.TripResponseDto;
import ma.youcode.covoiturage.entities.Stop;
import ma.youcode.covoiturage.entities.Trip;
import ma.youcode.covoiturage.entities.Vehicle;
import ma.youcode.covoiturage.mapper.StopMapper;
import ma.youcode.covoiturage.mapper.TripMapper;
import ma.youcode.covoiturage.repositories.TripRepository;
import ma.youcode.covoiturage.repositories.VehicalRepository;
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
    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final StopMapper stopMapper;
    private final VehicalRepository vehicalRepository;

    @Transactional
    public TripResponseDto createTrip(TripRequestDto dto){
        Vehicle vehicle = vehicalRepository.findById(dto.vehicleId())
                .orElseThrow(() -> new ResourcesNotFoundException("Couldn't find a vehicle with this id :" + dto.vehicleId()));

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

        return tripMapper.toResponse(created);
    }
}
