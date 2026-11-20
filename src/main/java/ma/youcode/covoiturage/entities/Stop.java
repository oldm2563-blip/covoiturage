package ma.youcode.covoiturage.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Stop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
