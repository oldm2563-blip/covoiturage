package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.enums.Status;
import ma.youcode.covoiturage.repositories.TripRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TripStatusScheduler {

    private final TripRepository tripRepository;

    @Scheduled(cron = "0 * * * * *")
    public void finishCompletedTrips() {

        int updatedTrips = tripRepository.finishDepartedTrips(
                Status.TERMINE,
                List.of(Status.PUBLIE, Status.COMPLET),
                LocalDateTime.now()
        );

        if (updatedTrips > 0) {
            System.out.println(
                    updatedTrips + " trips automatically marked as TERMINE"
            );
        }
    }
}