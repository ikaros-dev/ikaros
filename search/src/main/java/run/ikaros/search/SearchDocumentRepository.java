package run.ikaros.search;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SearchDocumentRepository extends ReactiveCrudRepository<SearchDocumentEntity, UUID> {
    @Query("select * from search_document where fields_json::text ilike :pattern "
        + "and (projected_at, document_id) < (:beforeAt, :beforeId) "
        + "order by projected_at desc, document_id desc limit :limit")
    Flux<SearchDocumentEntity> search(String pattern, java.time.Instant beforeAt, UUID beforeId, int limit);
}
