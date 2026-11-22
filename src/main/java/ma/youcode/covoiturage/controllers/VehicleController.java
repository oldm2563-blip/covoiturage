package ma.youcode.covoiturage.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.requests.VehicleRequestDto;
import ma.youcode.covoiturage.dto.response.VehicleResponseDto;
import ma.youcode.covoiturage.dto.updates.VehicleUpdateRequestDto;
import ma.youcode.covoiturage.services.VehicalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicalService vehicalService;

    @PostMapping
    public ResponseEntity<VehicleResponseDto> addVehicle(@Valid @RequestBody VehicleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicalService.addVehicle(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDto> editVehicle(@PathVariable Long id,
                                                          @Valid @RequestBody VehicleUpdateRequestDto dto) {
        return ResponseEntity.ok(vehicalService.editVehicle(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicalService.findById(id));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<VehicleResponseDto>> findByDriverId(@PathVariable Long driverId) {
        return ResponseEntity.ok(vehicalService.findByDriverId(driverId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicalService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}