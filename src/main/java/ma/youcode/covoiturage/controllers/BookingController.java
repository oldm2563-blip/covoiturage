package ma.youcode.covoiturage.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.requests.BookingRequestDto;
import ma.youcode.covoiturage.dto.response.BookingResponseDto;
import ma.youcode.covoiturage.services.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto dto) {

        BookingResponseDto response = bookingService.createBooking(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{bookingId}/passengers/{passengerId}")
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @PathVariable Long bookingId,
            @PathVariable Long passengerId) {

        BookingResponseDto response =
                bookingService.cancelBooking(bookingId, passengerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<List<BookingResponseDto>> getTripBookings(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return ResponseEntity.ok(
                bookingService.getTripBookings(tripId, driverId)
        );
    }

    @GetMapping("/passengers/{passengerId}")
    public ResponseEntity<List<BookingResponseDto>> getPassengerBookings(
            @PathVariable Long passengerId) {

        return ResponseEntity.ok(
                bookingService.getPassengerBookings(passengerId)
        );
    }
}