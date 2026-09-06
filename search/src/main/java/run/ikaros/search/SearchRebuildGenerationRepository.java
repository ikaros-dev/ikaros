package run.ikaros.search;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SearchRebuildGenerationRepository
    extends ReactiveCrudRepository<SearchRebuildGenerationEntity, String> { }
