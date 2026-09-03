package run.ikaros.music;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("music_queue_entry") public record MusicQueueEntryEntity(@Id UUID id,@Column("queue_id") UUID queueId,@Column("track_id") UUID trackId,@Column("base_position") int basePosition,@Column("active_position") int activePosition,@Column("inserted_by") UUID insertedBy,Instant createdAt) {}
