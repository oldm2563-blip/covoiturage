package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.common.exception.LicenseCodeAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.VehicleRequestDto;
import ma.youcode.covoiturage.dto.response.VehicleResponseDto;
import ma.youcode.covoiturage.dto.updates.VehicleUpdateRequestDto;
import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.entities.Vehicle;
import ma.youcode.covoiturage.mapper.VehicleMapper;
import ma.youcode.covoiturage.repositories.DriverRepository;
import ma.youcode.covoiturage.repositories.VehicalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicalService {

    private final VehicalRepository vehicalRepository;
    private final VehicleMapper vehicleMapper;
    private final DriverRepository driverRepository;

    public VehicleResponseDto addVehicle(VehicleRequestDto dto){

        if (vehicalRepository.existsByRegistrationNumber(dto.registrationNumber()))
            throw new LicenseCodeAlreadyExistsException("registration Number already Exists in the Database");

        if (dto.capacity() < 1 || dto.capacity() > 8)
            throw new LicenseCodeAlreadyExistsException("Capacity Should be between 1 to 8");

        Driver driver = driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new ResourcesNotFoundException("Driver not found"));

        Vehicle vehicle = vehicleMapper.toEntity(dto);
        vehicle.setDriver(driver);

        Vehicle created = vehicalRepository.save(vehicle);
        return vehicleMapper.toResponse(created);
    }

    public VehicleResponseDto editVehicle(Long id, VehicleUpdateRequestDto dto){

        Vehicle vehicle = vehicalRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Couldn't find a vehicle with this id :" + id));

        if (dto.registrationNumber() != null && !dto.registrationNumber().equals(vehicle.getRegistrationNumber()) && vehicalRepository.existsByRegistrationNumber(dto.registrationNumber()))
            throw new LicenseCodeAlreadyExistsException("registration Number already Exists in the Database");

        if (dto.capacity() != null && (dto.capacity() < 1 || dto.capacity() > 8))
            throw new LicenseCodeAlreadyExistsException("Capacity Should be between 1 to 8");

        vehicleMapper.updateVehicle(dto, vehicle);

        Vehicle updated = vehicalRepository.save(vehicle);

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
                .orElseThrow(() -> new ResourcesNotFoundException("Vehicle not found"));

        vehicalRepository.delete(vehicle);
    }
}
