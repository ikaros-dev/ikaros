package run.ikaros.server.core.file.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.server.core.file.event.FileChangeEvent;
import run.ikaros.server.core.file.event.FileRemoveEvent;
import run.ikaros.server.core.file.event.FileSaveEvent;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Component
public class SubtitleFileChangeListener {

    private final FileRepository fileRepository;

    public SubtitleFileChangeListener(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * Listener for subtitle file save and remove event.
     */
    @EventListener(FileChangeEvent.class)
    public Mono<Void> handle(FileChangeEvent event) {
        FileEntity fileEntity = event.getFileEntity();
        String fileEntityName = fileEntity.getName();
        String postfix = FileUtils.parseFilePostfix(fileEntityName);
        if (!"ass".equalsIgnoreCase(postfix)) {
            return Mono.empty();
        }

        if (event instanceof FileSaveEvent) {
            return handleSubtitleFileSaveEvent(fileEntity);
        }

        if (event instanceof FileRemoveEvent) {
            return handleSubtitleFileRemoveEvent(fileEntity);
        }
        return Mono.empty();
    }

    private static String substringFileEntityNamePrefix(FileEntity subtitleFileEntity) {
        String subtitleFileEntityName = subtitleFileEntity.getName();
        subtitleFileEntityName = subtitleFileEntityName
            .replace(FileUtils.parseFilePostfix(subtitleFileEntityName), "");
        subtitleFileEntityName =
            subtitleFileEntityName.substring(0, subtitleFileEntityName.length() - 1);
        subtitleFileEntityName = subtitleFileEntityName
            .replace(FileUtils.parseFilePostfix(subtitleFileEntityName), "");
        subtitleFileEntityName =
            subtitleFileEntityName.substring(0, subtitleFileEntityName.length() - 1);
        return subtitleFileEntityName;
    }

    /**
     * Add new video subtitle table records if not exists.
     */
    private Mono<Void> handleSubtitleFileSaveEvent(FileEntity subtitleFileEntity) {
        String subtitleFileEntityName = substringFileEntityNamePrefix(subtitleFileEntity);
        return Mono.empty();
        // todo refactor
        //return fileRepository.findAllByNameLike(subtitleFileEntityName + "%")
        //    .collectList().map(fileEntities -> fileEntities.get(0))
        //    .flatMap(fileEntity -> videoSubtitleRepository.findByVideoFileIdAndSubtitleFileId(
        //            fileEntity.getId(), subtitleFileEntity.getId())
        //        .switchIfEmpty(Mono.just(VideoSubtitleEntity.builder()
        //                .subtitleFileId(subtitleFileEntity.getId())
        //                .videoFileId(fileEntity.getId())
        //                .build())
        //            .flatMap(videoSubtitleEntity ->
        //                videoSubtitleRepository.save(videoSubtitleEntity)
        //                    .doOnSuccess(videoSubtitleEntity2 -> log.info(
        //                        "add new video => subtitle map record: [{}] => [{}].",
        //                        fileEntity.getName(), subtitleFileEntity.getName()))
        //            )
        //        )
        //    )
        //    .then();
    }

    /**
     * Remove video subtitle table record if exists.
     */
    private Mono<Void> handleSubtitleFileRemoveEvent(FileEntity subtitleFileEntity) {
        String subtitleFileEntityName = substringFileEntityNamePrefix(subtitleFileEntity);
        // todo refactor
        return Mono.empty();
        //return fileRepository.findAllByNameLike(subtitleFileEntityName + "%")
        //    .collectList().map(fileEntities -> fileEntities.get(0))
        //    .flatMap(fileEntity -> videoSubtitleRepository.findByVideoFileIdAndSubtitleFileId(
        //            fileEntity.getId(), subtitleFileEntity.getId())
        //  .flatMap(videoSubtitleEntity -> videoSubtitleRepository.delete(videoSubtitleEntity)
        //            .doOnSuccess(
        //                unused -> log.info("remove video => subtitle map record: [{}] => [{}].",
        //                    fileEntity.getName(), subtitleFileEntity.getName()))
        //        )
        //
        //    )
        //    .then();
    }
}
