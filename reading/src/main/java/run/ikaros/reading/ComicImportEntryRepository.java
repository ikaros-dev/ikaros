package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;

public interface ComicImportEntryRepository extends ReactiveCrudRepository<ComicImportEntryEntity, UUID> {
    Flux<ComicImportEntryEntity> findAllByImportIdOrderByChapterKeyAscPageOrderAsc(UUID importId);
    reactor.core.publisher.Mono<Void> deleteAllByImportId(UUID importId);
    @Modifying
    @Query("update reading_comic_import_entry set page_order = page_order + :offset, version = version + 1 where import_id = :importId and chapter_key = :chapterKey")
    reactor.core.publisher.Mono<Integer> shiftPageOrders(UUID importId, String chapterKey, int offset);
    @Modifying
    @Query("update reading_comic_import_entry set page_order = :pageOrder, version = version + 1 where import_id = :importId and id = :entryId")
    reactor.core.publisher.Mono<Integer> updatePageOrder(UUID importId, UUID entryId, int pageOrder);
}
