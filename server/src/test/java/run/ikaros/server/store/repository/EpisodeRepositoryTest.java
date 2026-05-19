package run.ikaros.server.store.repository;


import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.EpisodeEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class EpisodeRepositoryTest {

    @Autowired
    EpisodeRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeEntity entity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-1")
            .group(EpisodeGroup.MAIN)
            .sequence(1.0f)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllBySubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeEntity entity1 = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-1")
            .group(EpisodeGroup.MAIN)
            .sequence(1.0f)
            .build();
        entity1.setId(UuidV7Utils.generateUuid());

        EpisodeEntity entity2 = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-2")
            .group(EpisodeGroup.MAIN)
            .sequence(2.0f)
            .build();
        entity2.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllBySubjectId(subjectId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void countBySubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.countBySubjectId(subjectId))
            .expectNext(0L).verifyComplete();

        EpisodeEntity entity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-1")
            .group(EpisodeGroup.MAIN)
            .sequence(1.0f)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.countBySubjectId(subjectId))
            .expectNext(1L).verifyComplete();
    }

    @Test
    void deleteAllBySubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeEntity entity1 = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-1")
            .group(EpisodeGroup.MAIN)
            .sequence(1.0f)
            .build();
        entity1.setId(UuidV7Utils.generateUuid());

        EpisodeEntity entity2 = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-2")
            .group(EpisodeGroup.MAIN)
            .sequence(2.0f)
            .build();
        entity2.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.deleteAllBySubjectId(subjectId))
            .verifyComplete();

        StepVerifier.create(repository.findAllBySubjectId(subjectId))
            .expectNextCount(0).verifyComplete();
    }

    @Test
    void findBySubjectIdAndGroupAndSequence() {
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeEntity entity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-1")
            .group(EpisodeGroup.MAIN)
            .sequence(1.0f)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findBySubjectIdAndGroupAndSequence(
                subjectId, EpisodeGroup.MAIN, 1.0f))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findBySubjectIdAndGroupAndSequenceAndName() {
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeEntity entity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-1")
            .group(EpisodeGroup.MAIN)
            .sequence(1.0f)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findBySubjectIdAndGroupAndSequenceAndName(
                subjectId, EpisodeGroup.MAIN, 1.0f, "Episode-1"))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void deleteBySubjectIdAndGroupAndSequenceAndName() {
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeEntity entity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("Episode-1")
            .group(EpisodeGroup.MAIN)
            .sequence(1.0f)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.deleteBySubjectIdAndGroupAndSequenceAndName(
                subjectId, EpisodeGroup.MAIN, 1.0f, "Episode-1"))
            .verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNextCount(0).verifyComplete();
    }
}
