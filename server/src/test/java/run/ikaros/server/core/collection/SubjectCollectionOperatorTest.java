package run.ikaros.server.core.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

class SubjectCollectionOperatorTest {

    @Mock
    private SubjectCollectionService service;

    private SubjectCollectionOperator operator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        operator = new SubjectCollectionOperator(service);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(operator).isNotNull();
    }

    @Test
    void collectDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        CollectionType type = CollectionType.WISH;
        Boolean isPrivate = false;

        when(service.collect(userId, subjectId, type, isPrivate))
            .thenReturn(Mono.empty());

        StepVerifier.create(operator.collect(userId, subjectId, type, isPrivate))
            .verifyComplete();

        verify(service).collect(userId, subjectId, type, isPrivate);
    }

    @Test
    void findCollectionDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        SubjectCollection expected = SubjectCollection.builder()
            .userId(userId)
            .subjectId(subjectId)
            .type(CollectionType.WISH)
            .build();

        when(service.findCollection(userId, subjectId))
            .thenReturn(Mono.just(expected));

        StepVerifier.create(operator.findCollection(userId, subjectId))
            .assertNext(result -> {
                assertThat(result).isSameAs(expected);
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getSubjectId()).isEqualTo(subjectId);
            })
            .verifyComplete();

        verify(service).findCollection(userId, subjectId);
    }

    @Test
    void findCollectionsDelegatesToService() {
        UUID userId = UUID.randomUUID();
        int page = 1;
        int size = 20;
        PagingWrap<SubjectCollection> expected = new PagingWrap<>(page, size, 0, List.of());

        when(service.findCollections(userId, page, size))
            .thenReturn(Mono.just(expected));

        StepVerifier.create(operator.findCollections(userId, page, size))
            .assertNext(result -> {
                assertThat(result).isSameAs(expected);
                assertThat(result.getPage()).isEqualTo(page);
                assertThat(result.getSize()).isEqualTo(size);
            })
            .verifyComplete();

        verify(service).findCollections(userId, page, size);
    }

    @Test
    void findCollectionsWithTypeAndPrivateDelegatesToService() {
        UUID userId = UUID.randomUUID();
        int page = 1;
        int size = 20;
        CollectionType type = CollectionType.DOING;
        Boolean isPrivate = true;
        PagingWrap<SubjectCollection> expected = new PagingWrap<>(page, size, 0, List.of());

        when(service.findCollections(userId, page, size, type, isPrivate))
            .thenReturn(Mono.just(expected));

        StepVerifier.create(operator.findCollections(userId, page, size, type, isPrivate))
            .assertNext(result -> {
                assertThat(result).isSameAs(expected);
                assertThat(result.getPage()).isEqualTo(page);
                assertThat(result.getSize()).isEqualTo(size);
            })
            .verifyComplete();

        verify(service).findCollections(userId, page, size, type, isPrivate);
    }

    @Test
    void updateMainEpisodeProgressDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        Integer progress = 5;

        when(service.updateMainEpisodeProgress(userId, subjectId, progress))
            .thenReturn(Mono.empty());

        StepVerifier.create(operator.updateMainEpisodeProgress(userId, subjectId, progress))
            .verifyComplete();

        verify(service).updateMainEpisodeProgress(userId, subjectId, progress);
    }
}
