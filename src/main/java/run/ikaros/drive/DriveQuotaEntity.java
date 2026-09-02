package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_quota")
public record DriveQuotaEntity(@Id UUID spaceId, @Column("limit_bytes") long limitBytes,
    @Column("used_bytes") long usedBytes, @Column("reserved_bytes") long reservedBytes, @Version Long version) {}
