package ma.youcode.covoiturage.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StopResponseDto(
        Long id,
        String city,
        LocalDateTime dateTime,
        BigDecimal price,
        int orderIndex
) {}