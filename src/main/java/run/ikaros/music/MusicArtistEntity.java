package run.ikaros.music;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
@Table("music_artist") public record MusicArtistEntity(@Id UUID id, @Column("owner_id") UUID ownerId, @Column("resource_id") UUID resourceId, String kind, @Column("sort_name") String sortName, String disambiguation, @Version Long version) {}
