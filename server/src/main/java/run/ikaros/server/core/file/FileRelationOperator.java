package run.ikaros.server.core.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.FileRelationOperate;
import run.ikaros.api.core.file.FileRelations;
import run.ikaros.api.core.subject.Subtitle;
import run.ikaros.api.store.enums.FileRelationType;

@Slf4j
@Component
public class FileRelationOperator implements FileRelationOperate {
    private final FileRelationService fileRelationService;

    public FileRelationOperator(FileRelationService fileRelationService) {
        this.fileRelationService = fileRelationService;
    }

    @Override
    public Mono<FileRelations> findFileRelations(FileRelationType relationType, Long fileId) {
        return fileRelationService.findFileRelations(relationType, fileId);

    }

    @Override
    public Flux<Subtitle> findVideoSubtitles(Long fileId) {
        return fileRelationService.findVideoSubtitles(fileId);
    }
}
