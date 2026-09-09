package run.ikaros.music;
import java.util.UUID; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface MusicTrackAssociationService { Mono<MusicTrackAssociationView> create(UUID ownerId,UUID trackId,CreateMusicTrackAssociationRequest request); Flux<MusicTrackAssociationView> list(UUID ownerId,UUID trackId); }
