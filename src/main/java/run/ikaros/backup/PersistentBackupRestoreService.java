package run.ikaros.backup;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
@Service
public class PersistentBackupRestoreService implements BackupRestoreService {
    private final RestorePointRepository repository;
    public PersistentBackupRestoreService(RestorePointRepository repository){this.repository=repository;}
    @Override public Mono<RestorePointView> create(CreateRestorePointRequest req){Instant now=Instant.now();return repository.save(new RestorePointEntity(null,req.formatVersion(),req.sourceInstanceId(),req.schemaVersion(),req.manifestDigest(),RestorePointState.PREPARING,req.level(),VerificationStatus.NOT_VERIFIED,null,0,0,now,null,null)).map(this::view);}
    @Override public Flux<RestorePointView> list(){return repository.findAllByOrderByCreatedAtDesc().map(this::view);}
    @Override public Mono<RestorePointView> verify(UUID id,VerifyRestorePointRequest req){
        if (req.checkedObjects() < 0 || req.failedObjects() < 0 || req.failedObjects() > req.checkedObjects())
            return Mono.error(new IllegalArgumentException("验证对象计数不合法"));
        return required(id).flatMap(old->{if(old.state()==RestorePointState.PUBLISHED||old.state()==RestorePointState.RETIRED)return Mono.error(new ConflictException("已发布恢复点不可原地修改"));return repository.save(new RestorePointEntity(old.id(),old.formatVersion(),old.sourceInstanceId(),old.schemaVersion(),old.manifestDigest(),req.status()==VerificationStatus.PASSED?RestorePointState.VERIFYING:RestorePointState.FAILED,req.level(),req.status(),req.failureReason(),req.checkedObjects(),req.failedObjects(),old.createdAt(),null,old.version()));}).map(this::view);
    }
    @Override public Mono<RestorePointView> publish(UUID id){return required(id).flatMap(old->{if(old.state()==RestorePointState.PUBLISHED)return Mono.just(old);if(old.state()!=RestorePointState.VERIFYING||old.verificationStatus()!=VerificationStatus.PASSED)return Mono.error(new ConflictException("恢复点必须验证通过后才能发布"));Instant now=Instant.now();return repository.save(new RestorePointEntity(old.id(),old.formatVersion(),old.sourceInstanceId(),old.schemaVersion(),old.manifestDigest(),RestorePointState.PUBLISHED,old.verificationLevel(),old.verificationStatus(),old.failureReason(),old.checkedObjects(),old.failedObjects(),old.createdAt(),now,old.version()));}).map(this::view);}
    private Mono<RestorePointEntity> required(UUID id){return repository.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Restore Point 不存在")));}
    private RestorePointView view(RestorePointEntity r){return new RestorePointView(r.id(),r.formatVersion(),r.sourceInstanceId(),r.schemaVersion(),r.manifestDigest(),r.state(),r.verificationLevel(),r.verificationStatus(),r.failureReason(),r.checkedObjects(),r.failedObjects(),r.createdAt(),r.publishedAt());}
}
