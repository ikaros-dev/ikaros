package run.ikaros.resource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserResourceStateView(UUID userId, UUID resourceId, boolean favorite, BigDecimal rating,
                                    String statusCode, BigDecimal progressValue, String progressUnit,
                                    Instant lastAccessedAt, Long version, Instant updatedAt) {
}
