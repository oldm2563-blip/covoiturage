package ma.youcode.covoiturage.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.youcode.covoiturage.enums.Status;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToMany(mappedBy = "trip",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Stop> stops = new ArrayList<>();

    @Column(name = "seats_Offered" , nullable = false)
    private int seatsOffered;

    @ManyToOne(optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PUBLIE;

}
