package ma.youcode.covoiturage.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.requests.PassengerRequestDto;
import ma.youcode.covoiturage.dto.updates.PassengerUpdateRequestDto;
import ma.youcode.covoiturage.dto.response.PassengerResponseDto;
import ma.youcode.covoiturage.services.PassengerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passenger")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    public ResponseEntity<PassengerResponseDto> createPassenger(@Valid @RequestBody PassengerRequestDto dto){
        PassengerResponseDto responseDto = passengerService.addPassenger(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<PassengerResponseDto>> allPassenger(){
        return ResponseEntity.ok(passengerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponseDto> findById(@PathVariable long id){
        return ResponseEntity.ok(passengerService.getPassenger(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PassengerResponseDto> updatePassenger(@PathVariable Long id, @Valid @RequestBody PassengerUpdateRequestDto dto) {

        return ResponseEntity.ok(passengerService.updatePassenger(id, dto));
    }

    @GetMapping("/search")
    public ResponseEntity<PassengerResponseDto> searchByEmail(@RequestParam String email){
        return ResponseEntity.ok(passengerService.searchByEmail(email));
    }
}
