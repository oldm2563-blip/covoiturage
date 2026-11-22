package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.requests.TripRequestDto;
import ma.youcode.covoiturage.dto.response.TripResponseDto;
import ma.youcode.covoiturage.entities.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = StopMapper.class)
public interface TripMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "availableSeats", ignore = true)
    TripResponseDto toResponse(Trip trip);

    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "stops", ignore = true)
    Trip toEntity(TripRequestDto dto);
}
