package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.requests.PassengerRequestDto;
import ma.youcode.covoiturage.dto.updates.PassengerUpdateRequestDto;
import ma.youcode.covoiturage.dto.response.PassengerResponseDto;
import ma.youcode.covoiturage.entities.Passenger;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PassengerMapper {

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updatePassenger(
            PassengerUpdateRequestDto dto,
            @MappingTarget Passenger passenger
    );

    PassengerResponseDto toResponse(Passenger passenger);

    @Mapping(target = "id", ignore = true)
    Passenger toEntity(PassengerRequestDto request);

}
