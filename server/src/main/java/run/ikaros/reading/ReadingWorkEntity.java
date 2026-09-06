package run.ikaros.reading;
import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.annotation.Version; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("reading_work") public record ReadingWorkEntity(@Id UUID id,@Column("owner_id") UUID ownerId,@Column("resource_id") UUID resourceId,String kind,@Column("original_language") String originalLanguage,String serializationStatus,@Version Long version) {}
