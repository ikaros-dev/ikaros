package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface DriveFileRevisionRepository extends ReactiveCrudRepository<DriveFileRevisionEntity, UUID> {
    Flux<DriveFileRevisionEntity> findAllByFileNodeIdOrderByRevisionNoDesc(UUID fileNodeId);
    Mono<DriveFileRevisionEntity> findByFileNodeIdAndRevisionNo(UUID fileNodeId, long revisionNo);
    Mono<DriveFileRevisionEntity> findByFileNodeIdAndOperationId(UUID fileNodeId, String operationId);
}
