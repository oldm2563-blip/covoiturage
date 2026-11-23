package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.common.exception.BusinessRuleException;
import ma.youcode.covoiturage.common.exception.LicenseCodeAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.VehicleRequestDto;
import ma.youcode.covoiturage.dto.response.VehicleResponseDto;
import ma.youcode.covoiturage.dto.updates.VehicleUpdateRequestDto;
import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.entities.Vehicle;
import ma.youcode.covoiturage.mapper.VehicleMapper;
import ma.youcode.covoiturage.repositories.DriverRepository;
import ma.youcode.covoiturage.repositories.TripRepository;
import ma.youcode.covoiturage.repositories.VehicalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicalService {

    private final VehicalRepository vehicalRepository;
    private final VehicleMapper vehicleMapper;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;

    public VehicleResponseDto addVehicle(VehicleRequestDto dto){

        if (vehicalRepository.existsByRegistrationNumber(dto.registrationNumber()))
        {
            String reason = "registration Number already Exists in the Database";
            log.warn("Rule refused: {}", reason);
            throw new LicenseCodeAlreadyExistsException(reason);
        }

        if (dto.capacity() < 1 || dto.capacity() > 8)
        {
            String reason = "Capacity Should be between 1 to 8";
            log.warn("Rule refused: {}", reason);
            throw new LicenseCodeAlreadyExistsException(reason);
        }

        Driver driver = driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new ResourcesNotFoundException("Driver not found"));

        Vehicle vehicle = vehicleMapper.toEntity(dto);
        vehicle.setDriver(driver);

        Vehicle created = vehicalRepository.save(vehicle);
        log.info("Vehicle {} added for driver {}", created.getId(), driver.getId());
        return vehicleMapper.toResponse(created);
    }

    public VehicleResponseDto editVehicle(Long id, VehicleUpdateRequestDto dto){

        Vehicle vehicle = vehicalRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Couldn't find a vehicle with this id :" + id));

        if (dto.registrationNumber() != null && !dto.registrationNumber().equals(vehicle.getRegistrationNumber()) && vehicalRepository.existsByRegistrationNumber(dto.registrationNumber()))
        {
            String reason = "registration Number already Exists in the Database";
            log.warn("Rule refused: {}", reason);
            throw new LicenseCodeAlreadyExistsException(reason);
        }

        if (dto.capacity() != null && (dto.capacity() < 1 || dto.capacity() > 8))
        {
            String reason = "Capacity Should be between 1 to 8";
            log.warn("Rule refused: {}", reason);
            throw new LicenseCodeAlreadyExistsException(reason);
        }

        vehicleMapper.updateVehicle(dto, vehicle);

        Vehicle updated = vehicalRepository.save(vehicle);

        log.info("Vehicle {} updated", updated.getId());
        return vehicleMapper.toResponse(updated);
    }

    public VehicleResponseDto findById(Long id){
        Vehicle vehicle = vehicalRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Couldn't find a vehicle with this id :" + id));

        return vehicleMapper.toResponse(vehicle);
    }

    public List<VehicleResponseDto> findByDriverId(Long id){

        Driver driver = driverRepository.findById(id).orElseThrow(() -> new ResourcesNotFoundException("Couldn't find a driver with this id: " + id));
        return vehicalRepository
                .findAllByDriver(driver)
                .stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    public void deleteVehicle(Long id) {

        Vehicle vehicle = vehicalRepository.findById(id)
                .orElseThrow(() ->
                        new ResourcesNotFoundException("Vehicle not found"));

        if (tripRepository.existsByVehicle_Id(id)) {
            String reason = "Cannot delete a vehicle that is associated with trips";
            log.warn("Rule refused: {}", reason);
            throw new BusinessRuleException(reason);
        }

        vehicalRepository.delete(vehicle);
        log.info("Vehicle {} deleted", id);
    }
}