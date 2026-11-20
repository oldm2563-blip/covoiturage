package ma.youcode.covoiturage.dto.updates;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PassengerUpdateRequestDto(
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Email
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^(\\+212|0)[5-7][0-9]{8}$")
        String phone
) {}