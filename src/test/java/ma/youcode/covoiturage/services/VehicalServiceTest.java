package ma.youcode.covoiturage.services;

import ma.youcode.covoiturage.common.exception.BusinessRuleException;
import ma.youcode.covoiturage.common.exception.LicenseCodeAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.VehicleRequestDto;
import ma.youcode.covoiturage.dto.response.VehicleResponseDto;
import ma.youcode.covoiturage.dto.updates.VehicleUpdateRequestDto;
import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.entities.Vehicle;
import ma.youcode.covoiturage.mapper.VehicleMapper;
import ma.youcode.covoiturage.repositories.DriverRepository;
import ma.youcode.covoiturage.repositories.TripRepository;
import ma.youcode.covoiturage.repositories.VehicalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicalServiceTest {

    @Mock
    private VehicalRepository vehicalRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private VehicalService vehicalService;

    // ---------------- CREATE ----------------

    @Test
    void shouldCreateVehicleAndLinkItToItsDriver() {
        // Arrange
        VehicleRequestDto request = new VehicleRequestDto(
                "Toyota", "Corolla", "12345-A-1", "White", 4, 1L);
        Driver driver = new Driver();
        Vehicle vehicle = new Vehicle();
        VehicleResponseDto expected = new VehicleResponseDto(
                1L, "Toyota", "Corolla", "12345-A-1", "White", 4, 1L);

        when(vehicalRepository.existsByRegistrationNumber("12345-A-1")).thenReturn(false);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(vehicleMapper.toEntity(request)).thenReturn(vehicle);
        when(vehicalRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponse(vehicle)).thenReturn(expected);

        // Act
        VehicleResponseDto result = vehicalService.addVehicle(request);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(vehicle.getDriver()).isSameAs(driver);
    }

    @Test
    void shouldRejectVehicleWhenRegistrationNumberAlreadyExists() {
        // Arrange
        VehicleRequestDto request = new VehicleRequestDto(
                "Toyota", "Corolla", "TAKEN-1", "White", 4, 1L);
        when(vehicalRepository.existsByRegistrationNumber("TAKEN-1")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> vehicalService.addVehicle(request))
                .isInstanceOf(LicenseCodeAlreadyExistsException.class);

        verify(vehicalRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9})
    void shouldRejectVehicleWhenCapacityIsOutsideOneToEight(int invalidCapacity) {
        // Arrange
        VehicleRequestDto request = new VehicleRequestDto(
                "Toyota", "Corolla", "12345-A-1", "White", invalidCapacity, 1L);
        when(vehicalRepository.existsByRegistrationNumber("12345-A-1")).thenReturn(false);

        // Act + Assert
        // (the current code throws LicenseCodeAlreadyExistsException for a bad capacity)
        assertThatThrownBy(() -> vehicalService.addVehicle(request))
                .isInstanceOf(LicenseCodeAlreadyExistsException.class)
                .hasMessageContaining("Capacity");

        verify(vehicalRepository, never()).save(any());
    }

    @Test
    void shouldRejectVehicleWhenDriverDoesNotExist() {
        // Arrange
        VehicleRequestDto request = new VehicleRequestDto(
                "Toyota", "Corolla", "12345-A-1", "White", 4, 99L);
        when(vehicalRepository.existsByRegistrationNumber("12345-A-1")).thenReturn(false);
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> vehicalService.addVehicle(request))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(vehicalRepository, never()).save(any());
    }

    // ---------------- READ ----------------

    @Test
    void shouldReturnVehicleWhenIdExists() {
        // Arrange
        Vehicle vehicle = new Vehicle();
        VehicleResponseDto expected = new VehicleResponseDto(
                1L, "Toyota", "Corolla", "12345-A-1", "White", 4, 1L);

        when(vehicalRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleMapper.toResponse(vehicle)).thenReturn(expected);

        // Act
        VehicleResponseDto result = vehicalService.findById(1L);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenVehicleIsNotFound() {
        // Arrange
        when(vehicalRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> vehicalService.findById(99L))
                .isInstanceOf(ResourcesNotFoundException.class);
    }

    @Test
    void shouldReturnAllVehiclesOfADriver() {
        // Arrange
        Driver driver = new Driver();
        Vehicle vehicle = new Vehicle();
        VehicleResponseDto expected = new VehicleResponseDto(
                1L, "Toyota", "Corolla", "12345-A-1", "White", 4, 1L);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(vehicalRepository.findAllByDriver(driver)).thenReturn(List.of(vehicle));
        when(vehicleMapper.toResponse(vehicle)).thenReturn(expected);

        // Act
        List<VehicleResponseDto> result = vehicalService.findByDriverId(1L);

        // Assert
        assertThat(result).containsExactly(expected);
    }

    // ---------------- UPDATE ----------------

    @Test
    void shouldUpdateVehicleWhenNewRegistrationNumberIsAvailable() {
        // Arrange
        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber("OLD-1");
        VehicleUpdateRequestDto update = new VehicleUpdateRequestDto(
                "Toyota", "Yaris", "NEW-2", "Red", 4);
        VehicleResponseDto expected = new VehicleResponseDto(
                1L, "Toyota", "Yaris", "NEW-2", "Red", 4, 1L);

        when(vehicalRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicalRepository.existsByRegistrationNumber("NEW-2")).thenReturn(false);
        when(vehicalRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponse(vehicle)).thenReturn(expected);

        // Act
        VehicleResponseDto result = vehicalService.editVehicle(1L, update);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(vehicleMapper).updateVehicle(update, vehicle);
        verify(vehicalRepository).save(vehicle);
    }

    @Test
    void shouldThrowWhenUpdatingVehicleThatDoesNotExist() {
        // Arrange
        VehicleUpdateRequestDto update = new VehicleUpdateRequestDto(
                "Toyota", "Yaris", "NEW-2", "Red", 4);
        when(vehicalRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> vehicalService.editVehicle(99L, update))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(vehicalRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateWhenNewRegistrationNumberBelongsToAnotherVehicle() {
        // Arrange
        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber("OLD-1");
        VehicleUpdateRequestDto update = new VehicleUpdateRequestDto(
                "Toyota", "Yaris", "TAKEN-1", "Red", 4);

        when(vehicalRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicalRepository.existsByRegistrationNumber("TAKEN-1")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> vehicalService.editVehicle(1L, update))
                .isInstanceOf(LicenseCodeAlreadyExistsException.class);

        verify(vehicalRepository, never()).save(any());
    }

    // ---------------- DELETE ----------------

    @Test
    void shouldDeleteVehicleWhenItIsNotLinkedToAnyTrip() {
        // Arrange
        Vehicle vehicle = new Vehicle();
        when(vehicalRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(tripRepository.existsByVehicle_Id(1L)).thenReturn(false);

        // Act
        vehicalService.deleteVehicle(1L);

        // Assert
        verify(vehicalRepository).delete(vehicle);
    }

    @Test
    void shouldThrowWhenDeletingVehicleThatDoesNotExist() {
        // Arrange
        when(vehicalRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> vehicalService.deleteVehicle(99L))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(vehicalRepository, never()).delete(any());
    }

    @Test
    void shouldPreventDeletingVehicleLinkedToATrip() {
        // Arrange
        Vehicle vehicle = new Vehicle();
        when(vehicalRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(tripRepository.existsByVehicle_Id(1L)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> vehicalService.deleteVehicle(1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(vehicalRepository, never()).delete(any());
    }
}