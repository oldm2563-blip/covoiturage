package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.requests.DriverRequestDto;
import ma.youcode.covoiturage.dto.updates.DriverUpdateRequestDto;
import ma.youcode.covoiturage.dto.response.DriverResponseDto;
import ma.youcode.covoiturage.entities.Driver;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateDriver(
            DriverUpdateRequestDto dto,
            @MappingTarget Driver driver
    );

    DriverResponseDto toResponse(Driver driver);

    @Mapping(target = "id", ignore = true)
    Driver toEntity(DriverRequestDto request);
}
