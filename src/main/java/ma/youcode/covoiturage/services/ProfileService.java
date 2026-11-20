package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.repositories.DriverRepository;
import ma.youcode.covoiturage.repositories.PassengerRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;

    public boolean emailExists(String email) {
        return passengerRepository.existsByEmail(email)
                || driverRepository.existsByEmail(email);
    }
}
