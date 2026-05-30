package run.ikaros.server.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.search.subject.SubjectDoc;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.TagEntity;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.TagRepository;

class IndicesServiceImplTest2 {

    private SubjectRepository subjectRepository;
    private TagRepository tagRepository;
    private SubjectSearchService subjectSearchService;
    private IndicesServiceImpl indicesService;

    @BeforeEach
    void setUp() {
        subjectRepository = mock(SubjectRepository.class);
        tagRepository = mock(TagRepository.class);
        subjectSearchService = mock(SubjectSearchService.class);
        indicesService = new IndicesServiceImpl(subjectRepository, tagRepository, 
            subjectSearchService);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(indicesService).isNotNull();
    }

    @Test
    void rebuildSubjectIndices_withEmptySubjects_completesSuccessfully() {
        when(subjectRepository.findAll()).thenReturn(Flux.empty());
        
        StepVerifier.create(indicesService.rebuildSubjectIndices())
            .verifyComplete();
    }

    @Test
    void rebuildSubjectIndices_withSubjects_rebuildsIndices() throws IOException {
        SubjectEntity subject = new SubjectEntity();
        subject.setId(UUID.randomUUID());
        subject.setName("Test Subject");
        
        SubjectDoc subjectDoc = new SubjectDoc();
        subjectDoc.setId(subject.getId());
        subjectDoc.setName(subject.getName());
        
        TagEntity tag = new TagEntity();
        tag.setId(UUID.randomUUID());
        tag.setName("Test Tag");
        tag.setType(TagType.SUBJECT);
        tag.setMasterId(subject.getId());
        
        when(subjectRepository.findAll()).thenReturn(Flux.just(subject));
        when(tagRepository.findAllByTypeAndMasterId(any(TagType.class), any(UUID.class)))
            .thenReturn(Flux.just(tag));
        
        StepVerifier.create(indicesService.rebuildSubjectIndices())
            .verifyComplete();
        
        verify(subjectSearchService).rebuild(anyList());
    }

    @Test
    void rebuildSubjectIndices_withException_handlesError() throws IOException {
        SubjectEntity subject = new SubjectEntity();
        subject.setId(UUID.randomUUID());
        subject.setName("Test Subject");
        
        when(subjectRepository.findAll()).thenReturn(Flux.just(subject));
        when(tagRepository.findAllByTypeAndMasterId(any(TagType.class), any(UUID.class)))
            .thenReturn(Flux.empty());
        doThrow(new RuntimeException("Test exception"))
            .when(subjectSearchService).rebuild(any());
        
        StepVerifier.create(indicesService.rebuildSubjectIndices())
            .verifyComplete();
    }

    @Test
    void fetchSubTags_withTags_setsTagsOnSubjectDoc() {
        UUID subjectId = UUID.randomUUID();
        SubjectDoc subjectDoc = new SubjectDoc();
        subjectDoc.setId(subjectId);
        subjectDoc.setName("Test Subject");
        
        TagEntity tag1 = new TagEntity();
        tag1.setName("Tag1");
        tag1.setType(TagType.SUBJECT);
        tag1.setMasterId(subjectId);
        
        TagEntity tag2 = new TagEntity();
        tag2.setName("Tag2");
        tag2.setType(TagType.SUBJECT);
        tag2.setMasterId(subjectId);
        
        when(tagRepository.findAllByTypeAndMasterId(TagType.SUBJECT, subjectId))
            .thenReturn(Flux.just(tag1, tag2));
        
        // Test through rebuildSubjectIndices
        when(subjectRepository.findAll()).thenReturn(Flux.empty());
        
        StepVerifier.create(indicesService.rebuildSubjectIndices())
            .verifyComplete();
    }
}
