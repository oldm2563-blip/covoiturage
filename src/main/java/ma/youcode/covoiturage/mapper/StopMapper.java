package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.requests.StopRequestDto;
import ma.youcode.covoiturage.dto.response.StopResponseDto;
import ma.youcode.covoiturage.entities.Stop;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StopMapper {

    StopResponseDto toResponse(Stop stop);

    Stop toEntity(StopRequestDto dto);

}
