package run.ikaros.reading;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.ResourceService;

@Service
public class PersistentEbookBookInfoService implements EbookBookInfoService {
    private final EbookImportRepository imports;
    private final ReadingWorkRepository works;
    private final ReadingEditionRepository editions;
    private final ReadingChapterRepository chapters;
    private final ResourceService resources;

    public PersistentEbookBookInfoService(EbookImportRepository imports, ReadingWorkRepository works,
        ReadingEditionRepository editions, ReadingChapterRepository chapters, ResourceService resources) {
        this.imports = imports;
        this.works = works;
        this.editions = editions;
        this.chapters = chapters;
        this.resources = resources;
    }

    @Override
    public Mono<EbookBookInfoView> get(UUID ownerId, UUID importId) {
        return imports.findByIdAndOwnerId(importId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("电子书导入不存在或无权访问")))
            .flatMap(item -> works.findById(item.workId())
                .filter(work -> work.ownerId().equals(ownerId))
                .switchIfEmpty(Mono.error(new NotFoundException("电子书作品不存在或无权访问")))
                .flatMap(work -> Mono.zip(
                    resources.get(ownerId, work.resourceId()),
                    editions.findById(item.editionId())
                        .filter(edition -> edition.ownerId().equals(ownerId))
                        .switchIfEmpty(Mono.error(new NotFoundException("电子书版本不存在或无权访问"))),
                    chapters.findAllByOwnerIdAndEditionIdOrderBySortOrderAsc(ownerId, item.editionId()).count()
                ).map(values -> {
                    var resource = values.getT1();
                    var edition = values.getT2();
                    return new EbookBookInfoView(item.id(), item.workId(), item.editionId(),
                        resource.primaryTitle(), edition.language(), edition.publisher(), edition.source(),
                        values.getT3(), ComicImportStatus.valueOf(item.status()), item.errorCode(), item.errorMessage());
                })));
    }
}
