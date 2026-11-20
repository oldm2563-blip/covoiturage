package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.common.exception.EmailAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.LicenseCodeAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.DriverRequestDto;
import ma.youcode.covoiturage.dto.updates.DriverUpdateRequestDto;
import ma.youcode.covoiturage.dto.response.DriverResponseDto;
import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.mapper.DriverMapper;
import ma.youcode.covoiturage.repositories.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;
    private final ProfileService profileService;

    @Transactional
    public DriverResponseDto addDriver(DriverRequestDto dto){
        if (profileService.emailExists(dto.email())){
            throw new EmailAlreadyExistsException("Email already existe.");
        }

        if (driverRepository.existsByLicenseNumber(dto.licenseNumber())){
            throw new EmailAlreadyExistsException("License number already existe.");
        }

        Driver driver = driverMapper.toEntity(dto);
        Driver created = driverRepository.save(driver);
        return driverMapper.toResponse(created);
    }

    public List<DriverResponseDto> getAll(){
        return driverRepository.findAll()
                .stream()
                .map(driverMapper::toResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public DriverResponseDto getDriver(long id){

        Driver driver = driverRepository.findById(id).orElseThrow(()-> new ResourcesNotFoundException("Drivernot found with id: " + id));
        return driverMapper.toResponse(driver);
    }

    @Transactional
    public DriverResponseDto updateDriver(long id, DriverUpdateRequestDto dto){

        Driver driver = driverRepository.findById(id)
                .orElseThrow(()-> new ResourcesNotFoundException("Passenger not found with id: " + id));

        if (dto.email() != null && !dto.email().equals(driver.getEmail()) && profileService.emailExists(dto.email())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        if (dto.licenseNumber() != null && !dto.licenseNumber().equals(driver.getLicenseNumber()) && profileService.emailExists(dto.email())) {
            throw new LicenseCodeAlreadyExistsException("License Number Already Exist");
        }

        driverMapper.updateDriver(dto, driver);

        Driver updated = driverRepository.save(driver);

        return driverMapper.toResponse(updated);
    }

    public List<DriverResponseDto> searchForDriver(String word){
        return driverRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainsIgnoreCaseOrEmailIgnoreCase(word, word,word)
                .stream()
                .map(driverMapper::toResponse)
                .toList();
    }
}
