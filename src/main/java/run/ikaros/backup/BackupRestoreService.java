package run.ikaros.backup;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface BackupRestoreService {
    Mono<RestorePointView> create(CreateRestorePointRequest request);
    Flux<RestorePointView> list();
    Mono<RestorePointView> verify(UUID id, VerifyRestorePointRequest request);
    Mono<RestorePointView> publish(UUID id);
}
