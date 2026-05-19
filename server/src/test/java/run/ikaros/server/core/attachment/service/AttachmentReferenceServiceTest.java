package run.ikaros.server.core.attachment.service;


import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentReferenceServiceTest {

    @Autowired
    AttachmentReferenceService attachmentReferenceService;
    @Autowired
    AttachmentReferenceRepository attachmentReferenceRepository;
    @Autowired
    AttachmentRepository attachmentRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(attachmentReferenceRepository.deleteAll()).verifyComplete();
        StepVerifier.create(attachmentRepository.deleteAll()).verifyComplete();
    }

    @Test
    void save() {
        AttachmentEntity attachmentEntity = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name("test-attachment-ref")
            .type(AttachmentType.File)
            .parentId(ROOT_DIRECTORY_ID)
            .size(1024L)
            .updateTime(LocalDateTime.now())
            .build();

        StepVerifier.create(attachmentRepository.insert(attachmentEntity))
            .expectNext(attachmentEntity).verifyComplete();

        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReference attachmentReference = AttachmentReference.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(attachmentEntity.getId())
            .referenceId(referenceId)
            .build();

        StepVerifier.create(attachmentReferenceService.save(attachmentReference))
            .expectNextMatches(ref ->
                ref.getAttachmentId().equals(attachmentEntity.getId())
                    && ref.getReferenceId().equals(referenceId)
                    && ref.getType().equals(AttachmentReferenceType.SUBJECT))
            .verifyComplete();
    }

    @Test
    void findAllByTypeAndAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        StepVerifier.create(attachmentReferenceRepository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(attachmentReferenceService.findAllByTypeAndAttachmentId(
                AttachmentReferenceType.EPISODE, attachmentId))
            .expectNextMatches(ref ->
                ref.getAttachmentId().equals(attachmentId)
                    && ref.getReferenceId().equals(referenceId))
            .verifyComplete();
    }

    @Test
    void removeById() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        StepVerifier.create(attachmentReferenceRepository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(attachmentReferenceService.removeById(entity.getId()))
            .verifyComplete();

        StepVerifier.create(attachmentReferenceRepository.findById(entity.getId()))
            .expectNextCount(0).verifyComplete();
    }

    @Test
    void removeAllByTypeAndReferenceId() {
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReferenceEntity entity1 = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(UuidV7Utils.generateUuid())
            .referenceId(referenceId)
            .build();

        AttachmentReferenceEntity entity2 = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(UuidV7Utils.generateUuid())
            .referenceId(referenceId)
            .build();

        StepVerifier.create(attachmentReferenceRepository.insert(entity1))
            .expectNext(entity1).verifyComplete();
        StepVerifier.create(attachmentReferenceRepository.insert(entity2))
            .expectNext(entity2).verifyComplete();

        StepVerifier.create(attachmentReferenceService.removeAllByTypeAndReferenceId(
                AttachmentReferenceType.SUBJECT, referenceId))
            .verifyComplete();

        StepVerifier.create(attachmentReferenceRepository.findAllByTypeAndReferenceId(
                AttachmentReferenceType.SUBJECT, referenceId))
            .expectNextCount(0).verifyComplete();
    }

    @Test
    void removeByTypeAndAttachmentIdAndReferenceId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        StepVerifier.create(attachmentReferenceRepository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(attachmentReferenceService.removeByTypeAndAttachmentIdAndReferenceId(
                AttachmentReferenceType.SUBJECT, attachmentId, referenceId))
            .verifyComplete();

        StepVerifier.create(attachmentReferenceRepository.findByTypeAndAttachmentIdAndReferenceId(
                AttachmentReferenceType.SUBJECT, attachmentId, referenceId))
            .expectNextCount(0).verifyComplete();
    }
}
