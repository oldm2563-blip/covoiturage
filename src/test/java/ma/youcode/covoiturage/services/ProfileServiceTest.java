package ma.youcode.covoiturage.services;

import ma.youcode.covoiturage.repositories.DriverRepository;
import ma.youcode.covoiturage.repositories.PassengerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void shouldReportEmailAsTakenWhenAPassengerUsesIt() {
        // Arrange
        when(passengerRepository.existsByEmail("user@example.com")).thenReturn(true);

        // Act
        boolean result = profileService.emailExists("user@example.com");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldReportEmailAsTakenWhenADriverUsesIt() {
        // Arrange
        when(passengerRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(driverRepository.existsByEmail("user@example.com")).thenReturn(true);

        // Act
        boolean result = profileService.emailExists("user@example.com");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldReportEmailAsAvailableWhenNeitherPassengerNorDriverUsesIt() {
        // Arrange
        when(passengerRepository.existsByEmail("free@example.com")).thenReturn(false);
        when(driverRepository.existsByEmail("free@example.com")).thenReturn(false);

        // Act
        boolean result = profileService.emailExists("free@example.com");

        // Assert
        assertThat(result).isFalse();
    }
}