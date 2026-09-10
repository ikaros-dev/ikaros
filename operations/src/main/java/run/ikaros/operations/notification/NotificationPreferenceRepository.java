package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface NotificationPreferenceRepository extends ReactiveCrudRepository<NotificationPreferenceEntity, UUID> { }
