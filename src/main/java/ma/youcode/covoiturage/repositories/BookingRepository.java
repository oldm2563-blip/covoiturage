package ma.youcode.covoiturage.repositories;

import ma.youcode.covoiturage.entities.Booking;
import ma.youcode.covoiturage.entities.Trip;
import ma.youcode.covoiturage.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
    SELECT COALESCE(SUM(b.numberOfSeats), 0)
    FROM Booking b
    WHERE b.trip = :trip
    AND b.status IN :statuses
""")
    Long countOccupiedSeats(
            @Param("trip") Trip trip,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    List<Booking> findByTrip_Id(Long tripId);

    List<Booking> findByPassenger_Id(Long passengerId);

    boolean existsByPassenger_IdAndTrip_IdAndStatusIn(
            Long passengerId,
            Long tripId,
            Collection<BookingStatus> statuses
    );


}