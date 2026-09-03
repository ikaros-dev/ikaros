package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_space")
public record DriveSpaceEntity(@Id UUID id, @Column("owner_user_id") UUID ownerUserId,
    @Column("display_name") String displayName, @Column("root_node_id") UUID rootNodeId,
    @Column("change_generation") long changeGeneration, String state,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
