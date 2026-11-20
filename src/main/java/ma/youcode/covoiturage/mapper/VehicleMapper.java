package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.requests.VehicleRequestDto;
import ma.youcode.covoiturage.dto.response.VehicleResponseDto;
import ma.youcode.covoiturage.dto.updates.VehicleUpdateRequestDto;
import ma.youcode.covoiturage.entities.Vehicle;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateVehicle(
            VehicleUpdateRequestDto dto,
            @MappingTarget Vehicle vehicle
            );

    VehicleResponseDto toResponse(Vehicle vehicle);


    @Mapping(target = "id", ignore = true)
    Vehicle toEntity(VehicleRequestDto dto);

}
