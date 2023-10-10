package run.ikaros.server.core.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.FileRelations;
import run.ikaros.api.core.subject.Subtitle;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.FileRelationType;
import run.ikaros.server.store.entity.FileRelationEntity;
import run.ikaros.server.store.repository.FileRelationRepository;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Service
public class FileRelationServiceImpl implements FileRelationService {
    private final FileRepository fileRepository;
    private final FileRelationRepository fileRelationRepository;

    public FileRelationServiceImpl(FileRepository fileRepository, FileRelationRepository fileRelationRepository) {
        this.fileRepository = fileRepository;
        this.fileRelationRepository = fileRelationRepository;
    }

    @Override
    public Mono<FileRelations> findFileRelations(FileRelationType relationType, Long fileId) {
        Assert.notNull(relationType, "'relationType' must not null.");
        Assert.isTrue(fileId > 0, "'fileId' must > 0.");
        return fileRelationRepository.findAllByRelationTypeAndFileId(relationType, fileId)
            .map(FileRelationEntity::getRelationFileId)
            .collectList()
            .map(ids -> FileRelations.builder()
                .fileId(fileId)
                .relationType(relationType)
                .relationFileIds(ids)
                .build());
    }

    @Override
    public Flux<Subtitle> findVideoSubtitles(Long fileId) {
        Assert.isTrue(fileId > 0, "'fileId' must > 0.");
        return fileRelationRepository.findAllByRelationTypeAndFileId(FileRelationType.VIDEO_SUBTITLE, fileId)
            .map(FileRelationEntity::getRelationFileId)
            .flatMap(fileRepository::findById)
            .map(fileEntity -> Subtitle.builder()
                .fileId(fileEntity.getId())
                .name(fileEntity.getName())
                .url(fileEntity.getUrl())
                .build());
    }
}
