package ma.youcode.covoiturage.dto.response;

public record DriverResponseDto(
        long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String licenseNumber
) {
}
