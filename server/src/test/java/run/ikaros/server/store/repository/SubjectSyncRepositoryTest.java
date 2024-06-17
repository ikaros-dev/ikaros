package run.ikaros.server.store.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.entity.SubjectSyncEntity;

@SpringBootTest
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
            .subjectId(328609L)
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId("328609")
            .build();

        StepVerifier.create(subjectSyncRepository.save(subjectSyncEntity)
                .map(SubjectSyncEntity::getId))
            .expectNextMatches(id -> id > 0)
            .verifyComplete();

        StepVerifier.create(subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(
                328609L, SubjectSyncPlatform.BGM_TV, "328609"
            )).expectNextMatches(newEntity ->
                newEntity.getId().equals(subjectSyncEntity.getId())
                    && newEntity.getSubjectId().equals(subjectSyncEntity.getSubjectId())
                    && newEntity.getPlatform().equals(subjectSyncEntity.getPlatform()))
            .verifyComplete();

    }
}