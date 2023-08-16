package run.ikaros.server.core.collection;


import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.core.collection.EpisodeCollection;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@SpringBootTest
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
        Long userId = new Random().nextLong(0, Long.MAX_VALUE);
        Long episodeId = new Random().nextLong(0, Long.MAX_VALUE);
        Long subjectId = new Random().nextLong(0, Long.MAX_VALUE);
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());


        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.save(episodeEntity))
            .expectNextCount(1)
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
        Long userId = new Random().nextLong(0, Long.MAX_VALUE);
        Long episodeId = new Random().nextLong(0, Long.MAX_VALUE);
        Long subjectId = new Random().nextLong(0, Long.MAX_VALUE);
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());

        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.save(episodeEntity))
            .expectNextCount(1)
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
        Long userId = new Random().nextLong(0, Long.MAX_VALUE);
        Long episodeId = new Random().nextLong(0, Long.MAX_VALUE);
        Long subjectId = new Random().nextLong(0, Long.MAX_VALUE);
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());

        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.save(episodeEntity))
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
        Long userId = new Random().nextLong(0, Long.MAX_VALUE);
        Long episodeId = new Random().nextLong(0, Long.MAX_VALUE);
        Long subjectId = new Random().nextLong(0, Long.MAX_VALUE);
        EpisodeGroup episodeGroup = EpisodeGroup.MAIN;
        String episodeName = String.valueOf(new Random().nextDouble());

        // save episode
        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .name(episodeName)
            .subjectId(subjectId)
            .group(episodeGroup)
            .build();
        episodeEntity.setId(episodeId);
        StepVerifier.create(episodeRepository.save(episodeEntity))
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
}