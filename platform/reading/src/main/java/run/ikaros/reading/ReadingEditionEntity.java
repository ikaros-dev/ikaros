package run.ikaros.reading;
import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.annotation.Version; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("reading_edition") public record ReadingEditionEntity(@Id UUID id,@Column("owner_id") UUID ownerId,@Column("work_id") UUID workId,String name,String language,String publisher,String source,@Column("preference_weight") int preferenceWeight,String availability,@Version Long version) {}
