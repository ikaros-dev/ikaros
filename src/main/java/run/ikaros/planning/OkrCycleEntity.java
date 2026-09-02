package run.ikaros.planning;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.annotation.Version; import org.springframework.data.relational.core.mapping.Column; import org.springframework.data.relational.core.mapping.Table;
@Table("planning_okr_cycle") public record OkrCycleEntity(@Id UUID id,@Column("owner_id") UUID ownerId,String name,Instant startAt,Instant endAt,OkrStatus status,@Column("created_at") Instant createdAt,@Column("updated_at") Instant updatedAt,@Version Long version) {}
