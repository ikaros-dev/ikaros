package run.ikaros.search;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SearchProjectionFailureRepository
    extends ReactiveCrudRepository<SearchProjectionFailureEntity, UUID> { }
