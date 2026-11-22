package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.response.BookingResponseDto;
import ma.youcode.covoiturage.entities.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "passengerId", source = "passenger.id")
    @Mapping(target = "tripId", source = "trip.id")
    @Mapping(target = "boardingCity", source = "boardingStop.city")
    @Mapping(target = "alightingCity", source = "alightingStop.city")
    BookingResponseDto toResponse(Booking booking);
}
