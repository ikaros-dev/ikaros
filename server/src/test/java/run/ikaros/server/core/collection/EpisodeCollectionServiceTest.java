package run.ikaros.server.core.collection;


import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.collection.EpisodeCollection;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class EpisodeCollectionServiceTest {

    @Autowired
    EpisodeCollectionService episodeCollectionService;
    @Autowired
    EpisodeRepository episodeRepository;
    @Autowired
    EpisodeCollectionRepository episodeCollectionRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(episodeRepository.deleteAll()).verifyComplete();
        StepVerifier.create(episodeCollectionRepository.deleteAll()).verifyComplete();
    }

    @Test
    void create() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());


        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.insert(episodeEntity))
            .expectNext(episodeEntity)
            .verifyComplete();

        // find episode collection when not exists
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId))
            .verifyComplete();

        // create episode collection
        StepVerifier.create(episodeCollectionService.create(userId, episodeId))
            .expectNextCount(1)
            .verifyComplete();

        // find episode collection when exists
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId)
                .map(EpisodeCollection::getUserId))
            .expectNext(userId).verifyComplete();
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId)
                .map(EpisodeCollection::getEpisodeId))
            .expectNext(episodeId).verifyComplete();
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId)
                .map(EpisodeCollection::getName))
            .expectNext(episodeName).verifyComplete();

    }

    @Test
    void remove() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());

        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.insert(episodeEntity))
            .expectNext(episodeEntity)
            .verifyComplete();

        // create episode collection
        StepVerifier.create(episodeCollectionService.create(userId, episodeId))
            .expectNextCount(1)
            .verifyComplete();

        // find episode collection when exists
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId)
                .map(EpisodeCollection::getUserId))
            .expectNext(userId).verifyComplete();

        // remove episode collection
        StepVerifier.create(episodeCollectionService.remove(userId, episodeId))
            .expectNextCount(1)
            .verifyComplete();

        // find episode collection after remove
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId)
                .map(EpisodeCollection::getUserId))
            .verifyComplete();
    }

    @Test
    void updateEpisodeCollectionProgress() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());

        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.insert(episodeEntity))
            .expectNextCount(1)
            .verifyComplete();

        // create episode collection
        StepVerifier.create(episodeCollectionService.create(userId, episodeId))
            .expectNextCount(1)
            .verifyComplete();

        // find episode collection when exists
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId))
            .expectNextMatches(episodeCollection -> episodeCollection.getProgress() == null)
            .verifyComplete();

        // update episode collection progress
        Long progress = new Random().nextLong(0, Long.MAX_VALUE);
        StepVerifier.create(
                episodeCollectionService
                    .updateEpisodeCollectionProgress(userId, episodeId, progress))
            .verifyComplete();

        // find episode collection after update progress
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId))
            .expectNextMatches(
                episodeCollection -> progress.equals(episodeCollection.getProgress()))
            .verifyComplete();

    }

    @Test
    void updateEpisodeCollectionFinish() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());

        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.insert(episodeEntity))
            .expectNextCount(1)
            .verifyComplete();

        // create episode collection
        StepVerifier.create(episodeCollectionService.create(userId, episodeId))
            .expectNextCount(1)
            .verifyComplete();

        // find episode collection when exists
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId))
            .expectNextMatches(episodeCollection -> !episodeCollection.getFinish())
            .verifyComplete();

        // update episode collection finish
        Long progress = new Random().nextLong(0, Long.MAX_VALUE);
        StepVerifier.create(
                episodeCollectionService.updateEpisodeCollectionFinish(userId, episodeId, true))
            .verifyComplete();

        // find episode collection after update finish
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId))
            .expectNextMatches(EpisodeCollection::getFinish)
            .verifyComplete();
    }

    @Test
    void updateEpisodeCollection() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());

        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.insert(episodeEntity))
            .expectNextCount(1)
            .verifyComplete();

        // create episode collection
        StepVerifier.create(episodeCollectionService.create(userId, episodeId))
            .expectNextCount(1)
            .verifyComplete();

        // update episode collection with progress and duration
        Long progress = 120000L;
        Long duration = 300000L;
        StepVerifier.create(episodeCollectionService
                .updateEpisodeCollection(userId, episodeId, progress, duration))
            .verifyComplete();

        // find episode collection after update
        StepVerifier.create(episodeCollectionService.findByUserIdAndEpisodeId(userId, episodeId))
            .expectNextMatches(episodeCollection ->
                progress.equals(episodeCollection.getProgress())
                    && duration.equals(episodeCollection.getDuration()))
            .verifyComplete();
    }

    @Test
    void findAllByUserIdAndSubjectId() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;

        // Create multiple episodes
        for (int i = 0; i < 3; i++) {
            String episodeName = "episode-" + i;
            EpisodeEntity episodeEntity = EpisodeEntity.builder()
                .name(episodeName)
                .subjectId(subjectId)
                .group(episodeGroup)
                .sequence((float) i)
                .build();
            episodeEntity.setId(UuidV7Utils.generateUuid());
            StepVerifier.create(episodeRepository.insert(episodeEntity))
                .expectNextCount(1)
                .verifyComplete();

            // Create episode collection for each episode
            StepVerifier.create(episodeCollectionService.create(userId, episodeEntity.getId()))
                .expectNextCount(1)
                .verifyComplete();
        }

        // Find all episode collections for user and subject
        StepVerifier.create(episodeCollectionService.findAllByUserIdAndSubjectId(userId, subjectId))
            .expectNextCount(3)
            .verifyComplete();
    }
}