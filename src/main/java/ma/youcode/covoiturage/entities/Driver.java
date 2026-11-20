package ma.youcode.covoiturage.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@Entity
public class Driver extends Profile{

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;
}
