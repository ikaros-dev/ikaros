package run.ikaros.search;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SearchDocumentRepository extends ReactiveCrudRepository<SearchDocumentEntity, UUID> {
}
