package ma.youcode.covoiturage.entities;

import jakarta.persistence.*;
import lombok.*;



@MappedSuperclass
@Setter
@Getter
@NoArgsConstructor
public abstract class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id;

    @Column(name = "first_name", nullable = false)
    protected String firstName;

    @Column(name = "last_name", nullable = false)
    protected String lastName;

    @Column(nullable = false, unique = true)
    protected String email;

    @Column(nullable = false)
    protected String phone;
}
