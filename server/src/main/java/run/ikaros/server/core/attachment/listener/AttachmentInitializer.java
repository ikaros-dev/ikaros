package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
//@Component
public class AttachmentInitializer {
    private final AttachmentRepository repository;

    public AttachmentInitializer(AttachmentRepository repository) {
        this.repository = repository;
    }

    /**
     * Create default attachment.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initDefaultAttachments() {
        return repository.findById(ROOT_DIRECTORY_ID)
            .switchIfEmpty(repository.save(AttachmentEntity.builder()
                    .id(ROOT_DIRECTORY_ID)
                    .parentId(ROOT_DIRECTORY_ID)
                    .type(AttachmentType.Directory)
                    .path("/").name("/").updateTime(LocalDateTime.now())
                    .build())
                .doOnSuccess(entity ->
                    log.debug("Created default attachment {}", entity.getPath())))

            .then(repository.findByTypeAndParentIdAndName(
                AttachmentType.Directory, ROOT_DIRECTORY_ID, AttachmentConst.COVER_DIR_NAME))
            .switchIfEmpty(repository.save(AttachmentEntity.builder()
                    .id(AttachmentConst.COVER_DIRECTORY_ID)
                    .parentId(ROOT_DIRECTORY_ID)
                    .type(AttachmentType.Directory)
                    .path("/" + AttachmentConst.COVER_DIR_NAME)
                    .name(AttachmentConst.COVER_DIR_NAME)
                    .updateTime(LocalDateTime.now())
                    .build())
                .doOnSuccess(entity ->
                    log.debug("Created default attachment {}", entity.getPath())))

            .then(repository.findByTypeAndParentIdAndName(
                AttachmentType.Directory, ROOT_DIRECTORY_ID, AttachmentConst.DOWNLOAD_DIR_NAME))
            .switchIfEmpty(repository.save(AttachmentEntity.builder()
                    .id(AttachmentConst.DOWNLOAD_DIRECTORY_ID)
                    .parentId(ROOT_DIRECTORY_ID)
                    .type(AttachmentType.Directory)
                    .path("/" + AttachmentConst.DOWNLOAD_DIR_NAME)
                    .name(AttachmentConst.DOWNLOAD_DIR_NAME)
                    .updateTime(LocalDateTime.now())
                    .build())
                .doOnSuccess(entity ->
                    log.debug("Created default attachment {}", entity.getPath())))

            .then();
    }
}
