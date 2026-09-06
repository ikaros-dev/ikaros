package run.ikaros.ingestion;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;
public interface ImportConflictService {
    Mono<ImportConflictView> create(UUID ownerId, CreateImportConflictRequest request);
    Mono<List<ImportConflictView>> pending(UUID ownerId);
    Mono<ImportConflictView> resolve(UUID ownerId, UUID conflictId, ResolveImportConflictRequest request);
}
