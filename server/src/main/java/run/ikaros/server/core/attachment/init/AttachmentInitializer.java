package run.ikaros.server.core.attachment.init;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentInitializer {
    private final AttachmentRepository repository;

    public AttachmentInitializer(AttachmentRepository repository) {
        this.repository = repository;
    }

    /**
     * init some default attachment after application ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        // Root directory
        AttachmentEntity rootAttEntity = AttachmentEntity.builder()
            .id(AttachmentConst.ROOT_DIRECTORY_ID)
            .parentId(AttachmentConst.ROOT_DIRECTORY_PARENT_ID)
            .deleted(false)
            .name(AttachmentConst.ROOT_DIR_NAME)
            .type(AttachmentType.Directory)
            .fsPath("/")
            .path("/")
            .updateTime(LocalDateTime.now())
            .build();
        // Covers directory
        AttachmentEntity coverAttEntity = AttachmentEntity.builder()
            .id(AttachmentConst.COVER_DIRECTORY_ID)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .deleted(false)
            .name(AttachmentConst.COVER_DIR_NAME)
            .type(AttachmentType.Directory)
            .fsPath("/" + AttachmentConst.COVER_DIR_NAME)
            .path("/" + AttachmentConst.COVER_DIR_NAME)
            .updateTime(LocalDateTime.now())
            .build();
        // Downloads directory
        AttachmentEntity downloadsAttEntity = AttachmentEntity.builder()
            .id(AttachmentConst.DOWNLOAD_DIRECTORY_ID)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .deleted(false)
            .name(AttachmentConst.DOWNLOAD_DIR_NAME)
            .type(AttachmentType.Directory)
            .fsPath("/" + AttachmentConst.DOWNLOAD_DIR_NAME)
            .path("/" + AttachmentConst.DOWNLOAD_DIR_NAME)
            .updateTime(LocalDateTime.now())
            .build();
        Map<UUID, AttachmentEntity> idEntityMap =
            Map.of(rootAttEntity.getId(), rootAttEntity,
                coverAttEntity.getId(), coverAttEntity,
                downloadsAttEntity.getId(), downloadsAttEntity);
        return Flux.fromStream(idEntityMap.keySet().stream())
            .flatMap(uuid -> repository.existsById(uuid)
                .filter(exists -> !exists)
                .flatMap(e -> Mono.just(idEntityMap.get(uuid))))
            .flatMap(repository::insert)
            .doOnEach(attachmentEntitySignal -> {
                if (attachmentEntitySignal.isOnComplete() && attachmentEntitySignal.get() != null) {
                    log.debug("Insert attachment: {}", attachmentEntitySignal.get());
                }
            })
            .then();
    }

}
