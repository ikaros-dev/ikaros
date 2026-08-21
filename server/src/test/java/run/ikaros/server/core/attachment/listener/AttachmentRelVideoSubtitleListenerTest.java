package run.ikaros.server.core.attachment.listener;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.attachment.event.AttachmentReferenceSaveEvent;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentRelVideoSubtitleListenerTest {

    @Autowired
    private AttachmentRelVideoSubtitleListener listener;
    @Autowired
    AttachmentRepository repository;

    @Autowired
    AttachmentRelationRepository attachmentRelationRepository;
    @Autowired
    AttachmentReferenceRepository referenceRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
        StepVerifier.create(attachmentRelationRepository.deleteAll()).verifyComplete();
    }

    @Test
    void onAttachmentReferenceSaveEvent() {
        // 保存一些记录
        final String videoAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird "
                + "- Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].mkv";
        final String assScSubtitleAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird "
                + "- Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].sc.ass";
        final String assTcSubtitleAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird "
                + "- Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].tc.ass";

        AttachmentEntity videoAtt = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(videoAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.insert(videoAtt).map(AttachmentEntity::getId))
            .expectNext(videoAtt.getId())
            .verifyComplete();
        AttachmentEntity scSubtitleAtt = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(assScSubtitleAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.insert(scSubtitleAtt).map(AttachmentEntity::getId))
            .expectNext(scSubtitleAtt.getId())
            .verifyComplete();
        AttachmentEntity tcSubtitleAtt = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(assTcSubtitleAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.insert(tcSubtitleAtt).map(AttachmentEntity::getId))
            .expectNext(tcSubtitleAtt.getId())
            .verifyComplete();

        // 操作
        AttachmentReferenceEntity referenceEntity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(videoAtt.getId())
            .build();
        StepVerifier.create(referenceRepository.insert(referenceEntity))
            .expectNext(referenceEntity).verifyComplete();
        AttachmentReferenceSaveEvent event =
            new AttachmentReferenceSaveEvent(this, referenceEntity);

        StepVerifier.create(listener.onAttachmentReferenceSaveEvent(event)).verifyComplete();

        // 查询结果
        StepVerifier.create(attachmentRelationRepository.findAllByTypeAndAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, videoAtt.getId()
        ).collectList().map(List::size)).expectNext(2).verifyComplete();
    }
}