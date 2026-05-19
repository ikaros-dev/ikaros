package run.ikaros.server.core.tag.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.core.tag.event.TagChangeEvent;
import run.ikaros.server.search.IndicesProperties;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.TagEntity;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.TagRepository;

class TagEventListenerTest {

    private IndicesProperties indicesProperties;
    private SubjectRepository subjectRepository;
    private TagRepository tagRepository;
    private SubjectSearchService subjectSearchService;
    private TagChangeEventListener listener;

    @BeforeEach
    void setUp() {
        indicesProperties = new IndicesProperties();
        subjectRepository = mock(SubjectRepository.class);
        tagRepository = mock(TagRepository.class);
        subjectSearchService = mock(SubjectSearchService.class);
        listener = new TagChangeEventListener(indicesProperties, subjectRepository,
            tagRepository, subjectSearchService);
    }

    @Test
    void onTagCreateEvent_indicesDisabled_returnsEmpty() {
        indicesProperties.getInitializer().setEnabled(false);
        TagEntity tagEntity = buildTagEntity(TagType.SUBJECT);
        TagChangeEvent event = new TagChangeEvent(this, tagEntity);

        StepVerifier.create(listener.onTagCreateEvent(event))
            .verifyComplete();

        verify(subjectRepository, never()).findById(any(UUID.class));
    }

    @Test
    void onTagCreateEvent_nullEntity_returnsEmpty() {
        indicesProperties.getInitializer().setEnabled(true);
        TagChangeEvent event = new TagChangeEvent(this, (TagEntity) null);

        StepVerifier.create(listener.onTagCreateEvent(event))
            .verifyComplete();

        verify(subjectRepository, never()).findById(any(UUID.class));
    }

    @Test
    void onTagCreateEvent_nonSubjectType_returnsEmpty() {
        indicesProperties.getInitializer().setEnabled(true);
        TagEntity tagEntity = buildTagEntity(TagType.EPISODE);
        TagChangeEvent event = new TagChangeEvent(this, tagEntity);

        StepVerifier.create(listener.onTagCreateEvent(event))
            .verifyComplete();

        verify(subjectRepository, never()).findById(any(UUID.class));
    }

    @Test
    void onTagCreateEvent_subjectNotFound_returnsEmpty() {
        indicesProperties.getInitializer().setEnabled(true);
        UUID subjectId = UUID.randomUUID();
        TagEntity tagEntity = buildTagEntity(TagType.SUBJECT);
        tagEntity.setMasterId(subjectId);
        TagChangeEvent event = new TagChangeEvent(this, tagEntity);

        when(subjectRepository.findById(subjectId))
            .thenReturn(Mono.empty());

        StepVerifier.create(listener.onTagCreateEvent(event))
            .verifyComplete();

        verify(tagRepository, never()).findAllByTypeAndMasterId(any(), any());
    }

    @Test
    void onTagCreateEvent_subjectFound_updatesIndexDocument() throws Exception {
        indicesProperties.getInitializer().setEnabled(true);
        UUID subjectId = UUID.randomUUID();
        TagEntity tagEntity = buildTagEntity(TagType.SUBJECT);
        tagEntity.setMasterId(subjectId);
        final TagChangeEvent event = new TagChangeEvent(this, tagEntity);

        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setId(subjectId);
        subjectEntity.setName("Test Subject");
        subjectEntity.setAirTime(LocalDateTime.now());

        when(subjectRepository.findById(subjectId))
            .thenReturn(Mono.just(subjectEntity));

        TagEntity tag1 = buildTagEntity(TagType.SUBJECT);
        tag1.setName("tag1");
        TagEntity tag2 = buildTagEntity(TagType.SUBJECT);
        tag2.setName("tag2");

        when(tagRepository.findAllByTypeAndMasterId(eq(TagType.SUBJECT), eq(subjectId)))
            .thenReturn(Flux.just(tag1, tag2));

        StepVerifier.create(listener.onTagCreateEvent(event))
            .verifyComplete();

        verify(subjectSearchService).updateDocument(anyList());
    }

    @Test
    void onTagCreateEvent_updateDocumentThrows_errorIsPropagated() throws Exception {
        indicesProperties.getInitializer().setEnabled(true);
        UUID subjectId = UUID.randomUUID();
        TagEntity tagEntity = buildTagEntity(TagType.SUBJECT);
        tagEntity.setMasterId(subjectId);
        TagChangeEvent event = new TagChangeEvent(this, tagEntity);

        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setId(subjectId);
        subjectEntity.setName("Test Subject");
        subjectEntity.setAirTime(LocalDateTime.now());

        when(subjectRepository.findById(subjectId))
            .thenReturn(Mono.just(subjectEntity));

        TagEntity tag1 = buildTagEntity(TagType.SUBJECT);
        tag1.setName("tag1");

        when(tagRepository.findAllByTypeAndMasterId(eq(TagType.SUBJECT), eq(subjectId)))
            .thenReturn(Flux.just(tag1));

        org.mockito.Mockito.doThrow(new RuntimeException("search index error"))
            .when(subjectSearchService).updateDocument(anyList());

        StepVerifier.create(listener.onTagCreateEvent(event))
            .expectError(RuntimeException.class)
            .verify();
    }

    private TagEntity buildTagEntity(TagType type) {
        return TagEntity.builder()
            .id(UUID.randomUUID())
            .type(type)
            .name("testTag")
            .masterId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .createTime(LocalDateTime.now())
            .color("#FF0000")
            .build();
    }
}
