package ma.youcode.covoiturage.services;

import ma.youcode.covoiturage.common.exception.EmailAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.DriverRequestDto;
import ma.youcode.covoiturage.dto.response.DriverResponseDto;
import ma.youcode.covoiturage.dto.updates.DriverUpdateRequestDto;
import ma.youcode.covoiturage.entities.Driver;
import ma.youcode.covoiturage.mapper.DriverMapper;
import ma.youcode.covoiturage.repositories.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private DriverService driverService;

    // ---------------- CREATE ----------------

    @Test
    void shouldCreateDriverWhenEmailAndLicenseAreAvailable() {
        // Arrange
        DriverRequestDto request = new DriverRequestDto(
                "Ahmed", "Alaoui", "ahmed@example.com", "0612345678", "LIC-123");
        Driver driver = new Driver();
        DriverResponseDto expected = new DriverResponseDto(
                1L, "Ahmed", "Alaoui", "ahmed@example.com", "0612345678", "LIC-123");

        when(profileService.emailExists("ahmed@example.com")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("LIC-123")).thenReturn(false);
        when(driverMapper.toEntity(request)).thenReturn(driver);
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toResponse(driver)).thenReturn(expected);

        // Act
        DriverResponseDto result = driverService.addDriver(request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(driverRepository).save(driver);
    }

    @Test
    void shouldRejectDriverWhenEmailAlreadyExists() {
        // Arrange
        DriverRequestDto request = new DriverRequestDto(
                "Ahmed", "Alaoui", "taken@example.com", "0612345678", "LIC-123");
        when(profileService.emailExists("taken@example.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> driverService.addDriver(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(driverRepository, never()).save(any());
    }

    @Test
    void shouldRejectDriverWhenLicenseNumberAlreadyExists() {
        // Arrange
        DriverRequestDto request = new DriverRequestDto(
                "Ahmed", "Alaoui", "ahmed@example.com", "0612345678", "LIC-TAKEN");
        when(profileService.emailExists("ahmed@example.com")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("LIC-TAKEN")).thenReturn(true);

        // Act + Assert
        // (the current code throws EmailAlreadyExistsException for a duplicate license)
        assertThatThrownBy(() -> driverService.addDriver(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("License number");

        verify(driverRepository, never()).save(any());
    }

    // ---------------- READ ----------------

    @Test
    void shouldReturnDriverWhenIdExists() {
        // Arrange
        Driver driver = new Driver();
        DriverResponseDto expected = new DriverResponseDto(
                1L, "Ahmed", "Alaoui", "ahmed@example.com", "0612345678", "LIC-123");

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(driverMapper.toResponse(driver)).thenReturn(expected);

        // Act
        DriverResponseDto result = driverService.getDriver(1L);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenDriverIsNotFound() {
        // Arrange
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> driverService.getDriver(99L))
                .isInstanceOf(ResourcesNotFoundException.class);
    }

    // ---------------- UPDATE ----------------

    @Test
    void shouldUpdateDriverWhenNewEmailIsAvailable() {
        // Arrange
        Driver driver = new Driver();
        driver.setEmail("old@example.com");
        DriverUpdateRequestDto update = new DriverUpdateRequestDto(
                "Ahmed", null, "new@example.com", null, null);
        DriverResponseDto expected = new DriverResponseDto(
                1L, "Ahmed", "Alaoui", "new@example.com", "0612345678", "LIC-123");

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(profileService.emailExists("new@example.com")).thenReturn(false);
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toResponse(driver)).thenReturn(expected);

        // Act
        DriverResponseDto result = driverService.updateDriver(1L, update);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(driverMapper).updateDriver(update, driver);
        verify(driverRepository).save(driver);
    }

    @Test
    void shouldThrowWhenUpdatingDriverThatDoesNotExist() {
        // Arrange
        DriverUpdateRequestDto update = new DriverUpdateRequestDto(
                "Ahmed", null, null, null, null);
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> driverService.updateDriver(99L, update))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(driverRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateWhenNewEmailBelongsToAnotherUser() {
        // Arrange
        Driver driver = new Driver();
        driver.setEmail("old@example.com");
        DriverUpdateRequestDto update = new DriverUpdateRequestDto(
                null, null, "taken@example.com", null, null);

        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(profileService.emailExists("taken@example.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> driverService.updateDriver(1L, update))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(driverRepository, never()).save(any());
    }

    // ---------------- SEARCH ----------------

    @Test
    void shouldSearchDriversByNameOrEmailUsingTheSameKeyword() {
        // Arrange
        Driver driver = new Driver();
        DriverResponseDto expected = new DriverResponseDto(
                1L, "Ahmed", "Alaoui", "ahmed@example.com", "0612345678", "LIC-123");

        when(driverRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainsIgnoreCaseOrEmailIgnoreCase(
                        "ahmed", "ahmed", "ahmed"))
                .thenReturn(List.of(driver));
        when(driverMapper.toResponse(driver)).thenReturn(expected);

        // Act
        List<DriverResponseDto> result = driverService.searchForDriver("ahmed");

        // Assert
        assertThat(result).containsExactly(expected);
    }
}