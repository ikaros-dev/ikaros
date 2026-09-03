package run.ikaros.resource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_resource_state")
public record UserResourceStateEntity(@Column("user_id") UUID userId,
                                      @Column("resource_id") UUID resourceId,
                                      boolean favorite, BigDecimal rating,
                                      @Column("status_code") String statusCode,
                                      @Column("progress_value") BigDecimal progressValue,
                                      @Column("progress_unit") String progressUnit,
                                      @Column("last_accessed_at") Instant lastAccessedAt,
                                      @Version Long version, @Column("updated_at") Instant updatedAt) {
}
