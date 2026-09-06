package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/** 派生附件追溯关系的响应式持久化入口。 */
public interface DerivedAttachmentRepository extends ReactiveCrudRepository<DerivedAttachmentEntity, UUID> { }
