package ma.youcode.covoiturage.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.requests.TripRequestDto;
import ma.youcode.covoiturage.dto.requests.TripSearchRequestDto;
import ma.youcode.covoiturage.dto.response.TripResponseDto;
import ma.youcode.covoiturage.entities.Trip;
import ma.youcode.covoiturage.services.TripService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponseDto> publishTrip(@Valid @RequestBody TripRequestDto dto){
        TripResponseDto responseDto = tripService.createTrip(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDto> findTrip(@PathVariable Long id){
        TripResponseDto responseDto = tripService.getTripById(id);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/search")
    public Page<TripResponseDto> searchTrip(
            @Valid @ModelAttribute TripSearchRequestDto dto,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return tripService.searchTrips(dto, pageable);
    }

    @PatchMapping("/{tripId}/finish")
    public ResponseEntity<TripResponseDto> finishTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return ResponseEntity.ok(
                tripService.finishTrip(tripId, driverId)
        );
    }

    @PatchMapping("/{tripId}/cancel")
    public ResponseEntity<TripResponseDto> cancelTrip(
            @PathVariable Long tripId,
            @RequestParam Long driverId) {

        return ResponseEntity.ok(
                tripService.cancelTrip(tripId, driverId)
        );
    }
}
