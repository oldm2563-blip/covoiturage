package ma.youcode.covoiturage.repositories;

import ma.youcode.covoiturage.entities.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    boolean existsByEmail(String email);

    Optional<Passenger> findByEmailIgnoreCase(String email);
}
