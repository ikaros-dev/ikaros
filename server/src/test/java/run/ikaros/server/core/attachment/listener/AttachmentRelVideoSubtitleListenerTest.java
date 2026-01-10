package run.ikaros.server.core.attachment.listener;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.attachment.event.AttachmentReferenceSaveEvent;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
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
            .name(videoAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.save(videoAtt).map(AttachmentEntity::getId))
            .expectNextMatches(Objects::nonNull)
            .verifyComplete();
        AttachmentEntity scSubtitleAtt = AttachmentEntity.builder()
            .name(assScSubtitleAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.save(scSubtitleAtt).map(AttachmentEntity::getId))
            .expectNextMatches(Objects::nonNull)
            .verifyComplete();
        AttachmentEntity tcSubtitleAtt = AttachmentEntity.builder()
            .name(assTcSubtitleAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.save(tcSubtitleAtt).map(AttachmentEntity::getId))
            .expectNextMatches(Objects::nonNull)
            .verifyComplete();

        // 操作
        AttachmentReferenceSaveEvent event = new AttachmentReferenceSaveEvent(this,
            AttachmentReferenceEntity.builder()
                .type(AttachmentReferenceType.EPISODE)
                .attachmentId(videoAtt.getId())
                .build());

        StepVerifier.create(listener.onAttachmentReferenceSaveEvent(event)).verifyComplete();

        // 查询结果
        StepVerifier.create(attachmentRelationRepository.findAllByTypeAndAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, videoAtt.getId()
        ).collectList().map(List::size)).expectNext(2).verifyComplete();
    }
}