package run.ikaros.planning;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Column; import org.springframework.data.relational.core.mapping.Table;
@Table("planning_project_member") public record PlanningProjectMemberEntity(@Id UUID id,@Column("project_id") UUID projectId,@Column("user_id") UUID userId,PlanningProjectMemberRole role,@Column("created_at") Instant createdAt) {}
