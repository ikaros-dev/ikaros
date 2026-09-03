package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
public interface DriveQuotaRepository extends ReactiveCrudRepository<DriveQuotaEntity, UUID> {}
