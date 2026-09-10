package run.ikaros.music;
import java.util.UUID; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface MusicMetadataRecognitionService { Mono<MusicMetadataCandidateView> recognize(UUID ownerId,UUID trackId); Flux<MusicMetadataCandidateView> list(UUID ownerId,UUID trackId); }
