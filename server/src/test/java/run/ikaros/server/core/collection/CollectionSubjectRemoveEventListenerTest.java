package run.ikaros.server.core.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.SubjectCollectionRepository;

class CollectionSubjectRemoveEventListenerTest {

    @Mock
    private SubjectCollectionRepository subjectCollectionRepository;

    private CollectionSubjectRemoveEventListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new CollectionSubjectRemoveEventListener(subjectCollectionRepository);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(listener).isNotNull();
    }

    @Test
    void onSubjectAddDeletesCollectionsForRemovedSubject() {
        UUID subjectId = UUID.randomUUID();
        SubjectEntity entity = SubjectEntity.builder()
            .name("test-subject")
            .build();
        entity.setId(subjectId);

        SubjectRemoveEvent event = new SubjectRemoveEvent(this, entity);

        when(subjectCollectionRepository.removeAllBySubjectId(subjectId))
            .thenReturn(Mono.empty());

        StepVerifier.create(listener.onSubjectAdd(event))
            .verifyComplete();

        verify(subjectCollectionRepository).removeAllBySubjectId(subjectId);
    }

    @Test
    void onSubjectAddWithNullEntityDoesNothing() {
        SubjectRemoveEvent event = new SubjectRemoveEvent(this, null);

        StepVerifier.create(listener.onSubjectAdd(event))
            .verifyComplete();

        verify(subjectCollectionRepository, never()).removeAllBySubjectId(any());
    }
}
