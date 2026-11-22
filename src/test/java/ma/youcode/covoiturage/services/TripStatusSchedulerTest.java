package ma.youcode.covoiturage.services;

import ma.youcode.covoiturage.enums.Status;
import ma.youcode.covoiturage.repositories.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripStatusSchedulerTest {

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripStatusScheduler tripStatusScheduler;

    @Test
    void shouldMarkDepartedPublishedAndFullTripsAsFinished() {
        // Act
        tripStatusScheduler.finishCompletedTrips();

        // Assert: only PUBLIE and COMPLET trips are moved to TERMINE
        verify(tripRepository).finishDepartedTrips(
                eq(Status.TERMINE),
                eq(List.of(Status.PUBLIE, Status.COMPLET)),
                any(LocalDateTime.class));
    }
}