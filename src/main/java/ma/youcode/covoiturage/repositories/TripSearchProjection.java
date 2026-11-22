package ma.youcode.covoiturage.repositories;

import ma.youcode.covoiturage.entities.Trip;

public interface TripSearchProjection {
    Trip getTrip();
    Long getAvailableSeats();
}