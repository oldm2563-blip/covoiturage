
package ma.youcode.covoiturage.repositories;

import ma.youcode.covoiturage.entities.Trip;
import ma.youcode.covoiturage.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("""
        SELECT t
        FROM Trip t
        JOIN t.stops departure
        JOIN t.stops arrival
        WHERE departure.city = :departureCity
        AND arrival.city = :arrivalCity
        AND departure.orderIndex < arrival.orderIndex
        AND t.status = ma.youcode.covoiturage.enums.Status.PUBLIE

        AND (
            t.seatsOffered - COALESCE(
                (
                    SELECT SUM(b.numberOfSeats)
                    FROM Booking b
                    WHERE b.trip = t
                    AND b.status IN (
                        ma.youcode.covoiturage.enums.BookingStatus.PENDING,
                        ma.youcode.covoiturage.enums.BookingStatus.CONFIRMED
                    )
                ),
                0
            )
        ) >= :requestedSeats

        AND (
            :maxPrice IS NULL
            OR arrival.price - departure.price <= :maxPrice
        )

        AND departure.dateTime >= :startOfDay
        AND departure.dateTime < :startOfNextDay

        ORDER BY departure.dateTime ASC
        """)
    Page<Trip> searchTrips(
            @Param("departureCity") String departureCity,
            @Param("arrivalCity") String arrivalCity,
            @Param("requestedSeats") int requestedSeats,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay,
            Pageable pageable
    );

    boolean existsByVehicle_Id(Long vehicleId);

    @Modifying
    @Transactional
    @Query("""
    UPDATE Trip t
    SET t.status = :finishedStatus
    WHERE t.status IN :activeStatuses
    AND EXISTS (
        SELECT s
        FROM Stop s
        WHERE s.trip = t
        AND s.orderIndex = 0
        AND s.dateTime <= :now
    )
""")
    int finishDepartedTrips(
            @Param("finishedStatus") Status finishedStatus,
            @Param("activeStatuses") List<Status> activeStatuses,
            @Param("now") LocalDateTime now
    );

}
