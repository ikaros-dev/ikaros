package run.ikaros.server.core.collection;


import static run.ikaros.api.store.enums.CollectionType.WISH;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.exception.subject.SubjectNotFoundException;
import run.ikaros.api.infra.exception.user.UserNotFoundException;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.security.SecurityProperties;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.UserRepository;

@SpringBootTest
class SubjectCollectionServiceTest {

    @Autowired
    SubjectCollectionService subjectCollectionService;
    @Autowired
    SubjectCollectionRepository subjectCollectionRepository;
    @Autowired
    EpisodeRepository episodeRepository;
    @Autowired
    EpisodeCollectionRepository episodeCollectionRepository;
    @Autowired
    SubjectRepository subjectRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SecurityProperties securityProperties;

    void clear() {
        StepVerifier.create(subjectCollectionRepository.deleteAll()).verifyComplete();
        StepVerifier.create(episodeRepository.deleteAll()).verifyComplete();
        StepVerifier.create(episodeCollectionRepository.deleteAll()).verifyComplete();
        StepVerifier.create(subjectRepository.deleteAll()).verifyComplete();
    }

    @BeforeEach
    void setUp() {
        clear();
    }

    @AfterEach
    void tearDown() {
        clear();
    }

    private SubjectEntity randomAndSaveSubjectEntity() {
        SubjectEntity entity = SubjectEntity.builder()
            .name(String.valueOf(new Random().nextLong()))
            .nsfw(false)
            .type(SubjectType.ANIME)
            .airTime(LocalDateTime.now())
            .nameCn("")
            .build();

        StepVerifier.create(subjectRepository.save(entity))
            .expectNextCount(1).verifyComplete();

        return entity;
    }


    private List<EpisodeEntity> randomAndSaveEpisodeEntities(Long subjectId, Integer count) {
        List<EpisodeEntity> episodeEntities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String episodeName = String.valueOf(new Random().nextLong());
            EpisodeEntity episodeEntity = EpisodeEntity.builder()
                .group(EpisodeGroup.MAIN)
                .subjectId(subjectId)
                .name(episodeName)
                .airTime(LocalDateTime.now())
                .sequence(Float.sum(i, 1))
                .nameCn("")
                .build();

            StepVerifier.create(episodeRepository.save(episodeEntity))
                .expectNextCount(1).verifyComplete();

            episodeEntities.add(episodeEntity);
        }
        return episodeEntities;
    }

    private Long getDefaultUserId() {
        String masterUsername = securityProperties.getInitializer().getMasterUsername();
        UserEntity userEntity =
            userRepository.findByUsernameAndEnableAndDeleteStatus(masterUsername, true, false)
                .block(AppConst.BLOCK_TIMEOUT);
        return Objects.requireNonNull(userEntity).getId();
    }

    @Test
    void collect() {
        SubjectEntity subjectEntity = randomAndSaveSubjectEntity();
        Long subjectId = subjectEntity.getId();
        Long userId = getDefaultUserId();
        randomAndSaveEpisodeEntities(subjectId, 10);

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.collect(userId, subjectId, WISH))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .expectNextMatches(subjectCollection ->
                WISH.equals(subjectCollection.getType())
                    && subjectId.equals(subjectCollection.getSubjectId())
                    && userId.equals(subjectCollection.getUserId()))
            .verifyComplete();

    }

    @Test
    void collectWithSubjectCollection() {
        SubjectEntity subjectEntity = randomAndSaveSubjectEntity();
        Long subjectId = subjectEntity.getId();
        Long userId = getDefaultUserId();
        randomAndSaveEpisodeEntities(subjectId, 10);

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.collect(SubjectCollection.builder()
                .userId(userId).subjectId(subjectId).type(WISH).isPrivate(false).score(10)
                .build()))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .expectNextMatches(subjectCollection ->
                WISH.equals(subjectCollection.getType())
                    && subjectId.equals(subjectCollection.getSubjectId())
                    && userId.equals(subjectCollection.getUserId()))
            .verifyComplete();

    }

    @Test
    void unCollect() {
        SubjectEntity subjectEntity = randomAndSaveSubjectEntity();
        Long subjectId = subjectEntity.getId();
        Long userId = getDefaultUserId();
        randomAndSaveEpisodeEntities(subjectId, 10);

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.collect(userId, subjectId, WISH))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .expectNextMatches(subjectCollection ->
                WISH.equals(subjectCollection.getType())
                    && subjectId.equals(subjectCollection.getSubjectId())
                    && userId.equals(subjectCollection.getUserId()))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.unCollect(userId, subjectId))
            .verifyComplete();

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .verifyComplete();
    }

    @Test
    void findCollections() {
        final Long userId = getDefaultUserId();
        final int total = 100;
        for (int i = 0; i < total; i++) {
            SubjectEntity entity = SubjectEntity.builder()
                .name(String.valueOf(i))
                .nsfw(false)
                .type(SubjectType.ANIME)
                .airTime(LocalDateTime.now())
                .nameCn("")
                .build();

            StepVerifier.create(subjectRepository.save(entity))
                .expectNextCount(1).verifyComplete();

            Long subjectId = entity.getId();
            randomAndSaveEpisodeEntities(subjectId, 10);

            StepVerifier.create(subjectCollectionService.collect(userId, subjectId, WISH))
                .verifyComplete();
        }

        int page = 1;
        int size = 20;
        StepVerifier.create(subjectCollectionService.findCollections(userId, page, size))
            .expectNextMatches(predicate
                -> page == predicate.getPage()
                && size == predicate.getSize()
                && total == predicate.getTotal())
            .verifyComplete();
    }

    @Test
    void updateMainEpisodeProgress() {
        // user id not exists
        StepVerifier.create(subjectCollectionService.updateMainEpisodeProgress(1000L, 1000L, 0))
            .expectError(UserNotFoundException.class)
            .verify();
        // subject id not exists
        StepVerifier.create(
                subjectCollectionService.updateMainEpisodeProgress(getDefaultUserId(), 1000L, 0))
            .expectError(SubjectNotFoundException.class)
            .verify();

        // create data
        SubjectEntity subjectEntity = randomAndSaveSubjectEntity();
        Long subjectId = subjectEntity.getId();
        Long userId = getDefaultUserId();
        randomAndSaveEpisodeEntities(subjectId, 10);
        final int progress = new Random().nextInt(1, 9);

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .verifyComplete();

        // subject collection not found
        StepVerifier.create(
            subjectCollectionService.updateMainEpisodeProgress(userId, subjectId, progress)
        ).expectError(NotFoundException.class).verify();

        // collect subject for userid
        StepVerifier.create(subjectCollectionService.collect(userId, subjectId, WISH))
            .verifyComplete();

        StepVerifier.create(
            subjectCollectionService.updateMainEpisodeProgress(userId, subjectId, progress)
        ).verifyComplete();

        StepVerifier.create(subjectCollectionService.findCollection(userId, subjectId))
            .expectNextMatches(subjectCollection ->
                progress == subjectCollection.getMainEpisodeProgress())
            .verifyComplete();
    }
}