package run.ikaros.ingestion;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
@Service
public class DefaultImportConflictService implements ImportConflictService {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final ImportConflictRepository repository;
    public DefaultImportConflictService(ImportConflictRepository repository) { this.repository=repository; }
    public Mono<ImportConflictView> create(UUID ownerId, CreateImportConflictRequest request) {
        return repository.save(new ImportConflictEntity(null,request.planId(),request.candidateId(),ownerId,request.reason(),request.confidence(),ImportConflictStatus.OPEN.name(),null,Instant.now(),null,null)).map(this::view);
    }
    public Mono<List<ImportConflictView>> pending(UUID ownerId) { return repository.findAllByOwnerIdAndStatusOrderByCreatedAtAsc(ownerId,ImportConflictStatus.OPEN.name())
        .take(MAX_UNPAGED_RESULTS).map(this::view).collectList(); }
    public Mono<ImportConflictView> resolve(UUID ownerId,UUID id,ResolveImportConflictRequest request) {
        return repository.findByIdAndOwnerId(id,ownerId).switchIfEmpty(Mono.error(new NotFoundException("冲突不存在或无权访问")))
            .flatMap(current -> { if (!ImportConflictStatus.OPEN.name().equals(current.status())) return Mono.error(new ConflictException("冲突已处理"));
                if (current.version()!=null && !current.version().equals(request.expectedVersion())) return Mono.error(new ConflictException("冲突版本已过期"));
                String status="IGNORE".equalsIgnoreCase(request.resolution())?ImportConflictStatus.IGNORED.name():ImportConflictStatus.RESOLVED.name();
                return repository.save(new ImportConflictEntity(current.id(),current.planId(),current.candidateId(),current.ownerId(),current.reason(),current.confidence(),status,request.resolution(),current.createdAt(),Instant.now(),current.version()));
            }).map(this::view);
    }
    private ImportConflictView view(ImportConflictEntity c){return new ImportConflictView(c.id(),c.planId(),c.candidateId(),c.reason(),c.confidence(),ImportConflictStatus.valueOf(c.status()),c.resolution(),c.version(),c.createdAt(),c.resolvedAt());}
}
