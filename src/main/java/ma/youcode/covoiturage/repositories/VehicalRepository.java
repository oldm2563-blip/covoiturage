package ma.youcode.covoiturage.repositories;

import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicalRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByRegistrationNumber(String registrationNumber);
    List<Vehicle> findAllByDriver(Driver driver);
}
