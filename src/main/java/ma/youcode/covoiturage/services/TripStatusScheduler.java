package ma.youcode.covoiturage.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.enums.Status;
import ma.youcode.covoiturage.repositories.TripRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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
            log.info("{} trips automatically marked as TERMINE", updatedTrips);
        }
    }
}