package ma.youcode.covoiturage.services;

import ma.youcode.covoiturage.common.exception.EmailAlreadyExistsException;
import ma.youcode.covoiturage.common.exception.ResourcesNotFoundException;
import ma.youcode.covoiturage.dto.requests.PassengerRequestDto;
import ma.youcode.covoiturage.dto.response.PassengerResponseDto;
import ma.youcode.covoiturage.dto.updates.PassengerUpdateRequestDto;
import ma.youcode.covoiturage.entities.Passenger;
import ma.youcode.covoiturage.mapper.PassengerMapper;
import ma.youcode.covoiturage.repositories.PassengerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerServiceTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private PassengerMapper passengerMapper;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private PassengerService passengerService;

    // ---------------- CREATE ----------------

    @Test
    void shouldCreatePassengerWhenEmailIsAvailable() {
        // Arrange
        PassengerRequestDto request = new PassengerRequestDto(
                "Sara", "Idrissi", "sara@example.com", "0612345678");
        Passenger passenger = new Passenger();
        PassengerResponseDto expected = new PassengerResponseDto(
                1L, "Sara", "Idrissi", "sara@example.com", "0612345678");

        when(profileService.emailExists("sara@example.com")).thenReturn(false);
        when(passengerMapper.toEntity(request)).thenReturn(passenger);
        when(passengerRepository.save(passenger)).thenReturn(passenger);
        when(passengerMapper.toResponse(passenger)).thenReturn(expected);

        // Act
        PassengerResponseDto result = passengerService.addPassenger(request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(passengerRepository).save(passenger);
    }

    @Test
    void shouldRejectPassengerWhenEmailAlreadyExists() {
        // Arrange
        PassengerRequestDto request = new PassengerRequestDto(
                "Sara", "Idrissi", "taken@example.com", "0612345678");
        when(profileService.emailExists("taken@example.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> passengerService.addPassenger(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(passengerRepository, never()).save(any());
    }

    // ---------------- READ ----------------

    @Test
    void shouldReturnPassengerWhenIdExists() {
        // Arrange
        Passenger passenger = new Passenger();
        PassengerResponseDto expected = new PassengerResponseDto(
                1L, "Sara", "Idrissi", "sara@example.com", "0612345678");

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(passengerMapper.toResponse(passenger)).thenReturn(expected);

        // Act
        PassengerResponseDto result = passengerService.getPassenger(1L);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenPassengerIsNotFound() {
        // Arrange
        when(passengerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> passengerService.getPassenger(99L))
                .isInstanceOf(ResourcesNotFoundException.class);
    }

    // ---------------- UPDATE ----------------

    @Test
    void shouldUpdatePassengerWhenNewEmailIsAvailable() {
        // Arrange
        Passenger passenger = new Passenger();
        passenger.setEmail("old@example.com");
        PassengerUpdateRequestDto update = new PassengerUpdateRequestDto(
                "Sara", null, "new@example.com", null);
        PassengerResponseDto expected = new PassengerResponseDto(
                1L, "Sara", "Idrissi", "new@example.com", "0612345678");

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(profileService.emailExists("new@example.com")).thenReturn(false);
        when(passengerRepository.save(passenger)).thenReturn(passenger);
        when(passengerMapper.toResponse(passenger)).thenReturn(expected);

        // Act
        PassengerResponseDto result = passengerService.updatePassenger(1L, update);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(passengerMapper).updatePassenger(update, passenger);
        verify(passengerRepository).save(passenger);
    }

    @Test
    void shouldThrowWhenUpdatingPassengerThatDoesNotExist() {
        // Arrange
        PassengerUpdateRequestDto update = new PassengerUpdateRequestDto(
                "Sara", null, null, null);
        when(passengerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> passengerService.updatePassenger(99L, update))
                .isInstanceOf(ResourcesNotFoundException.class);

        verify(passengerRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateWhenNewEmailBelongsToAnotherUser() {
        // Arrange
        Passenger passenger = new Passenger();
        passenger.setEmail("old@example.com");
        PassengerUpdateRequestDto update = new PassengerUpdateRequestDto(
                null, null, "taken@example.com", null);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(profileService.emailExists("taken@example.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> passengerService.updatePassenger(1L, update))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(passengerRepository, never()).save(any());
    }

    // ---------------- SEARCH BY EMAIL ----------------

    @Test
    void shouldFindPassengerByEmail() {
        // Arrange
        Passenger passenger = new Passenger();
        PassengerResponseDto expected = new PassengerResponseDto(
                1L, "Sara", "Idrissi", "sara@example.com", "0612345678");

        when(passengerRepository.findByEmailIgnoreCase("sara@example.com"))
                .thenReturn(Optional.of(passenger));
        when(passengerMapper.toResponse(passenger)).thenReturn(expected);

        // Act
        PassengerResponseDto result = passengerService.searchByEmail("sara@example.com");

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenNoPassengerHasThisEmail() {
        // Arrange
        when(passengerRepository.findByEmailIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> passengerService.searchByEmail("unknown@example.com"))
                .isInstanceOf(ResourcesNotFoundException.class);
    }
}