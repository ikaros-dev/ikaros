package run.ikaros.server.store.repository;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.SubjectSyncEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class SubjectSyncRepositoryTest {

    @Autowired
    private SubjectSyncRepository subjectSyncRepository;

    @BeforeEach
    void setUp() {
        StepVerifier.create(subjectSyncRepository.deleteAll()).verifyComplete();
    }

    @Test
    void findBySubjectIdAndPlatformAndPlatformId() {
        SubjectSyncEntity subjectSyncEntity = SubjectSyncEntity.builder()
            .subjectId(UuidV7Utils.generateUuid())
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId("328609")
            .build();

        StepVerifier.create(subjectSyncRepository.save(subjectSyncEntity)
                .map(SubjectSyncEntity::getId))
            .expectNextMatches(Objects::nonNull)
            .verifyComplete();

        StepVerifier.create(subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(
                UuidV7Utils.generateUuid(), SubjectSyncPlatform.BGM_TV, "328609"
            )).expectNextMatches(newEntity ->
                newEntity.getId().equals(subjectSyncEntity.getId())
                    && newEntity.getSubjectId().equals(subjectSyncEntity.getSubjectId())
                    && newEntity.getPlatform().equals(subjectSyncEntity.getPlatform()))
            .verifyComplete();

    }
}