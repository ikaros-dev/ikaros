package run.ikaros.resource;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UserResourceStateRequest(boolean favorite,
    @DecimalMin("0") @DecimalMax("10") BigDecimal rating,
    String statusCode, @PositiveOrZero BigDecimal progressValue, String progressUnit) {
}
