package ma.youcode.covoiturage.common.exception;

public class LicenseCodeAlreadyExistsException extends RuntimeException {
    public LicenseCodeAlreadyExistsException(String message) {
        super(message);
    }
}
