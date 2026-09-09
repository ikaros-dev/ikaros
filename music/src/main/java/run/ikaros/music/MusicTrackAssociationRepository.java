package run.ikaros.music;
import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Mono;
public interface MusicTrackAssociationRepository extends ReactiveCrudRepository<MusicTrackAssociationEntity,UUID> { Mono<MusicTrackAssociationEntity> findByOwnerIdAndTrackIdAndCandidateId(UUID ownerId,UUID trackId,UUID candidateId); reactor.core.publisher.Flux<MusicTrackAssociationEntity> findAllByOwnerIdAndTrackId(UUID ownerId,UUID trackId); }
