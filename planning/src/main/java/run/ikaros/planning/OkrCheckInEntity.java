package run.ikaros.planning;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Column; import org.springframework.data.relational.core.mapping.Table;
@Table("planning_okr_check_in") public record OkrCheckInEntity(@Id UUID id,@Column("owner_id") UUID ownerId,@Column("key_result_id") UUID keyResultId,@Column("current_value") Double currentValue,Double progress,OkrConfidence confidence,String note,String blocker,@Column("created_at") Instant createdAt) {}
