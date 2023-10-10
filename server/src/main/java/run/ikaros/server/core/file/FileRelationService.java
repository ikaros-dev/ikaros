package run.ikaros.server.core.file;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.FileRelations;
import run.ikaros.api.core.subject.Subtitle;
import run.ikaros.api.store.enums.FileRelationType;

public interface FileRelationService {

    Mono<FileRelations> findFileRelations(FileRelationType relationType, Long fileId);

    Flux<Subtitle> findVideoSubtitles(Long fileId);
}
