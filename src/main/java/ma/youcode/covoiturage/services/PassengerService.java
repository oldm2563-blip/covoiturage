package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.common.exception.EmailAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.PassengerRequestDto;
import ma.youcode.covoiturage.dto.updates.PassengerUpdateRequestDto;
import ma.youcode.covoiturage.dto.response.PassengerResponseDto;
import ma.youcode.covoiturage.entities.Passenger;
import ma.youcode.covoiturage.mapper.PassengerMapper;
import ma.youcode.covoiturage.repositories.PassengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PassengerService {
    private final PassengerRepository passengerRepository;
    private final PassengerMapper passengerMapper;
    private final ProfileService profileService;

    @Transactional
    public PassengerResponseDto addPassenger(PassengerRequestDto dto){

        if (profileService.emailExists(dto.email())){
            throw new EmailAlreadyExistsException("Email already existe.");
        }

        Passenger passenger = passengerMapper.toEntity(dto);
        Passenger created = passengerRepository.save(passenger);
        return  passengerMapper.toResponse(created);
    }

    public List<PassengerResponseDto> getAll(){
        return passengerRepository.findAll()
                .stream()
                .map(passengerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PassengerResponseDto getPassenger(long id){

        Passenger passenger = passengerRepository.findById(id).orElseThrow(()-> new ResourcesNotFoundException("Passenger not found with id: " + id));
        return passengerMapper.toResponse(passenger);
    }

    @Transactional
    public PassengerResponseDto updatePassenger(long id, PassengerUpdateRequestDto dto){

        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(()-> new ResourcesNotFoundException("Passenger not found with id: " + id));

        if (dto.email() != null && !dto.email().equals(passenger.getEmail()) && profileService.emailExists(dto.email())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        passengerMapper.updatePassenger(dto, passenger);

        Passenger updated = passengerRepository.save(passenger);

        return passengerMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public PassengerResponseDto searchByEmail(String email){
        Passenger passenger = passengerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourcesNotFoundException("Passenger not found"));
        return passengerMapper.toResponse(passenger);
    }

}
