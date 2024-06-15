package run.ikaros.server.store.repository;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.store.entity.AttachmentEntity;

@SpringBootTest
class AttachmentRepositoryTest {


    @Autowired
    AttachmentRepository repository;


    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
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
            .expectNextMatches(id -> id != null && id > 0)
            .verifyComplete();
        StepVerifier.create(repository.save(AttachmentEntity.builder()
                .name(assScSubtitleAttName).type(AttachmentType.File).path(videoAttName)
                .build()).map(AttachmentEntity::getId))
            .expectNextMatches(id -> id != null && id > 0)
            .verifyComplete();
        StepVerifier.create(repository.save(AttachmentEntity.builder()
                .name(assTcSubtitleAttName).type(AttachmentType.File).path(videoAttName)
                .build()).map(AttachmentEntity::getId))
            .expectNextMatches(id -> id != null && id > 0)
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