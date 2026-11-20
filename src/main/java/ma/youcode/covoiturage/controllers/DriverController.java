package ma.youcode.covoiturage.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.requests.DriverRequestDto;
import ma.youcode.covoiturage.dto.updates.DriverUpdateRequestDto;
import ma.youcode.covoiturage.dto.response.DriverResponseDto;
import ma.youcode.covoiturage.services.DriverService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/Driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<DriverResponseDto> createDriver(@Valid @RequestBody DriverRequestDto dto){
        DriverResponseDto responseDto = driverService.addDriver(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<DriverResponseDto>> allDrivers(){
        return ResponseEntity.ok(driverService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDto> findById(@PathVariable long id){
        return ResponseEntity.ok(driverService.getDriver(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DriverResponseDto> updateDriver(@PathVariable Long id, @Valid @RequestBody DriverUpdateRequestDto dto) {

        return ResponseEntity.ok(driverService.updateDriver(id, dto));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DriverResponseDto>> searchForDriver(@RequestParam String word){
        return ResponseEntity.ok(driverService.searchForDriver(word));
    }
}
