package run.ikaros.server.core.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.collection.vo.FindCollectionCondition;
import run.ikaros.api.store.enums.CollectionCategory;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.entity.SubjectCollectionEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;

class DefaultCollectionServiceTest {
    private SubjectCollectionRepository subjectCollectionRepository;
    private EpisodeCollectionRepository episodeCollectionRepository;
    private UserService userService;
    private R2dbcEntityTemplate template;
    private DefaultCollectionService defaultCollectionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID episodeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        subjectCollectionRepository = Mockito.mock(SubjectCollectionRepository.class);
        episodeCollectionRepository = Mockito.mock(EpisodeCollectionRepository.class);
        userService = Mockito.mock(UserService.class);
        template = Mockito.mock(R2dbcEntityTemplate.class);
        defaultCollectionService = new DefaultCollectionService(
            subjectCollectionRepository, episodeCollectionRepository,
            userService, template
        );
    }

    @Test
    void findTypeBySubjectId() {
        SubjectCollectionEntity entity = new SubjectCollectionEntity();
        entity.setUserId(userId);
        entity.setSubjectId(subjectId);
        entity.setType(CollectionType.WISH);

        when(userService.getUserIdFromSecurityContext()).thenReturn(Mono.just(userId));
        when(subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId))
            .thenReturn(Mono.just(entity));

        StepVerifier
            .create(defaultCollectionService.findTypeBySubjectId(subjectId))
            .assertNext(type -> assertThat(type).isEqualTo(CollectionType.WISH))
            .verifyComplete();
    }

    @Test
    void findTypeBySubjectId_whenNotFound() {
        when(userService.getUserIdFromSecurityContext()).thenReturn(Mono.just(userId));
        when(subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId))
            .thenReturn(Mono.empty());

        StepVerifier
            .create(defaultCollectionService.findTypeBySubjectId(subjectId))
            .verifyComplete();
    }

    @Test
    void listCollectionsByCondition_forSubject_withType() {
        final FindCollectionCondition condition = FindCollectionCondition
            .builder()
            .page(1)
            .size(10)
            .category(CollectionCategory.SUBJECT)
            .type(CollectionType.DONE)
            .build();

        SubjectCollectionEntity entity = new SubjectCollectionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setSubjectId(subjectId);
        entity.setType(CollectionType.DONE);

        when(template.select(any(Query.class), eq(SubjectCollectionEntity.class)))
            .thenReturn(Flux.just(entity));
        when(template.count(any(Query.class), eq(SubjectCollectionEntity.class)))
            .thenReturn(Mono.just(1L));
        when(subjectCollectionRepository.findById(entity.getId()))
            .thenReturn(Mono.just(entity));

        StepVerifier
            .create(defaultCollectionService.listCollectionsByCondition(condition))
            .assertNext(pagingWrap -> {
                assertThat(pagingWrap.getPage()).isEqualTo(1);
                assertThat(pagingWrap.getSize()).isEqualTo(10);
                assertThat(pagingWrap.getTotal()).isEqualTo(1);
                List<?> items = pagingWrap.getItems();
                assertThat(items).hasSize(1);
            })
            .verifyComplete();
    }

    @Test
    void listCollectionsByCondition_forSubject_withoutType() {
        final FindCollectionCondition condition = FindCollectionCondition
            .builder()
            .page(1)
            .size(10)
            .category(CollectionCategory.SUBJECT)
            .build();

        SubjectCollectionEntity entity = new SubjectCollectionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setSubjectId(subjectId);
        entity.setType(CollectionType.WISH);

        when(template.select(any(Query.class), eq(SubjectCollectionEntity.class)))
            .thenReturn(Flux.just(entity));
        when(template.count(any(Query.class), eq(SubjectCollectionEntity.class)))
            .thenReturn(Mono.just(1L));
        when(subjectCollectionRepository.findById(entity.getId()))
            .thenReturn(Mono.just(entity));

        StepVerifier
            .create(defaultCollectionService.listCollectionsByCondition(condition))
            .assertNext(pagingWrap -> {
                assertThat(pagingWrap.getTotal()).isEqualTo(1);
            })
            .verifyComplete();
    }

    @Test
    void listCollectionsByCondition_forEpisode_withTimeRange() {
        long now = System.currentTimeMillis();
        final FindCollectionCondition condition = FindCollectionCondition
            .builder()
            .page(1)
            .size(10)
            .category(CollectionCategory.EPISODE)
            .updateTimeDesc(true)
            .time((now - 86400000) + "-" + now)
            .build();

        EpisodeCollectionEntity entity = new EpisodeCollectionEntity();
        entity.setId(episodeId);
        entity.setUserId(userId);
        entity.setEpisodeId(UUID.randomUUID());
        entity.setFinish(true);

        when(template.select(any(Query.class), eq(EpisodeCollectionEntity.class)))
            .thenReturn(Flux.just(entity));
        when(template.count(any(Query.class), eq(EpisodeCollectionEntity.class)))
            .thenReturn(Mono.just(1L));
        when(episodeCollectionRepository.findById(episodeId))
            .thenReturn(Mono.just(entity));

        StepVerifier
            .create(defaultCollectionService.listCollectionsByCondition(condition))
            .assertNext(pagingWrap -> {
                assertThat(pagingWrap.getTotal()).isEqualTo(1);
            })
            .verifyComplete();
    }

    @Test
    void listCollectionsByCondition_withPageZero_throwsException() {
        FindCollectionCondition condition = FindCollectionCondition
            .builder()
            .page(0)
            .size(10)
            .category(CollectionCategory.SUBJECT)
            .build();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> defaultCollectionService.listCollectionsByCondition(condition));
    }

    @Test
    void listCollectionsByCondition_withNullCondition_throwsException() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> defaultCollectionService.listCollectionsByCondition(null));
    }

    @Test
    void listCollectionsByCondition_forEpisode_withoutUpdateTimeDesc() {
        final FindCollectionCondition condition = FindCollectionCondition
            .builder()
            .page(1)
            .size(10)
            .category(CollectionCategory.EPISODE)
            .updateTimeDesc(false)
            .build();

        EpisodeCollectionEntity entity = new EpisodeCollectionEntity();
        entity.setId(episodeId);

        when(template.select(any(Query.class), eq(EpisodeCollectionEntity.class)))
            .thenReturn(Flux.just(entity));
        when(template.count(any(Query.class), eq(EpisodeCollectionEntity.class)))
            .thenReturn(Mono.just(1L));
        when(episodeCollectionRepository.findById(episodeId))
            .thenReturn(Mono.just(entity));

        StepVerifier
            .create(defaultCollectionService.listCollectionsByCondition(condition))
            .assertNext(pagingWrap -> assertThat(pagingWrap.getTotal()).isEqualTo(1))
            .verifyComplete();
    }
}
