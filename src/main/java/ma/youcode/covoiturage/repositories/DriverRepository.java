package ma.youcode.covoiturage.repositories;

import ma.youcode.covoiturage.entities.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByFirstNameContainingIgnoreCaseOrLastNameContainsIgnoreCaseOrEmailIgnoreCase(String firstName, String lastName, String email);
}
