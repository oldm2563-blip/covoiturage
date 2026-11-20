package ma.youcode.covoiturage.dto.response;

public record PassengerResponseDto(
        long id,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
