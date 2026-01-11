package run.ikaros.server.store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.AttachmentEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentRepositoryTest {


    @Autowired
    AttachmentRepository repository;

    @Test
    void findById() {
        final String name = String.valueOf(new Random().nextInt(9999));
        AttachmentEntity att = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .type(AttachmentType.Directory)
            .updateTime(LocalDateTime.now())
            .path("").fsPath("")
            .build();
        StepVerifier.create(repository.insert(att))
            .expectNext(att).verifyComplete();

        StepVerifier.create(repository.findById(att.getId()))
            .expectNext(att).verifyComplete();
    }

    @Test
    void findAllByTypeAndNameLike() {
        // 保存一些记录
        final String videoAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird "
                + "- Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].mkv";
        final String assScSubtitleAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird - "
                + "Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].sc.ass";
        final String assTcSubtitleAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird - "
                + "Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].tc.ass";

        StepVerifier.create(repository.save(AttachmentEntity.builder()
                .name(videoAttName).type(AttachmentType.File).path(videoAttName)
                .build()).map(AttachmentEntity::getId))
            .expectNextMatches(Objects::nonNull)
            .verifyComplete();
        StepVerifier.create(repository.save(AttachmentEntity.builder()
                .name(assScSubtitleAttName).type(AttachmentType.File).path(videoAttName)
                .build()).map(AttachmentEntity::getId))
            .expectNextMatches(Objects::nonNull)
            .verifyComplete();
        StepVerifier.create(repository.save(AttachmentEntity.builder()
                .name(assTcSubtitleAttName).type(AttachmentType.File).path(videoAttName)
                .build()).map(AttachmentEntity::getId))
            .expectNextMatches(Objects::nonNull)
            .verifyComplete();


        // 查询记录 验证结果
        String fileName = FileUtils.parseFileNameWithoutPostfix(videoAttName);
        Assertions.assertThat(fileName)
            .isEqualTo(
                "[Airota&LoliHouse] Liz and the Blue Bird "
                    + "- Movie [BDRip 1080p HEVC-yuv420p10 FLACx2]");
        StepVerifier.create(repository.findAllByTypeAndNameLike(
                AttachmentType.File, fileName + "%"
            ).collectList().map(List::size))
            .expectNext(3).verifyComplete();
    }
}