package run.ikaros.music;
import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface MusicMetadataCandidateRepository extends ReactiveCrudRepository<MusicMetadataCandidateEntity,UUID> { Mono<MusicMetadataCandidateEntity> findByOwnerIdAndTrackIdAndAttachmentId(UUID ownerId,UUID trackId,UUID attachmentId); Flux<MusicMetadataCandidateEntity> findAllByOwnerIdAndTrackIdOrderByCreatedAtDesc(UUID ownerId,UUID trackId); }
