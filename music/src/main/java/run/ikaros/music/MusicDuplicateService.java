package run.ikaros.music;
import java.util.UUID; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface MusicDuplicateService { Mono<java.util.List<MusicDuplicateView>> find(UUID ownerId,UUID attachmentId); }
