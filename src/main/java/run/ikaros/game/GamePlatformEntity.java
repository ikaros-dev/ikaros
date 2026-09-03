package run.ikaros.game; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("game_platform") public record GamePlatformEntity(@Id UUID id,@Column("owner_id") UUID ownerId,String name,String family,String architecture) {}
