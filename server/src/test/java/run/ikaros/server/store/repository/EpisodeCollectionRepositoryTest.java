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
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class EpisodeCollectionRepositoryTest {

    @Autowired
    EpisodeCollectionRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeCollectionEntity entity = EpisodeCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .episodeId(episodeId)
            .subjectId(subjectId)
            .finish(false)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findByUserIdAndEpisodeId() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeCollectionEntity entity = EpisodeCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .episodeId(episodeId)
            .subjectId(subjectId)
            .finish(false)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByUserIdAndEpisodeId(userId, episodeId))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllByUserIdAndSubjectId() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();

        EpisodeCollectionEntity entity1 = EpisodeCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .episodeId(UuidV7Utils.generateUuid())
            .subjectId(subjectId)
            .finish(false)
            .build();

        EpisodeCollectionEntity entity2 = EpisodeCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .episodeId(UuidV7Utils.generateUuid())
            .subjectId(subjectId)
            .finish(false)
            .build();

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByUserIdAndSubjectId(userId, subjectId))
            .expectNextCount(2).verifyComplete();
    }
}
