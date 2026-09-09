package run.ikaros.music;
import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository;
public interface MusicTrackArtistRepository extends ReactiveCrudRepository<MusicTrackArtistEntity,UUID> {}
