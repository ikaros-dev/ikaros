package run.ikaros.server.core.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.CharacterRepository;
import run.ikaros.server.store.repository.PersonRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;

class IkarosAppInfoContributorTest {

    private AttachmentRepository attachmentRepository;
    private SubjectRepository subjectRepository;
    private CharacterRepository characterRepository;
    private PersonRepository personRepository;
    private SubjectCollectionRepository subjectCollectionRepository;
    private IkarosAppInfoContributor contributor;

    @BeforeEach
    void setUp() {
        attachmentRepository = mock(AttachmentRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        characterRepository = mock(CharacterRepository.class);
        personRepository = mock(PersonRepository.class);
        subjectCollectionRepository = mock(SubjectCollectionRepository.class);
        contributor = new IkarosAppInfoContributor(
            attachmentRepository, subjectRepository, characterRepository,
            personRepository, subjectCollectionRepository);
    }

    @Test
    void contribute_populatesDetails() {
        // Given - mock all attachment repository count methods
        when(attachmentRepository.count()).thenReturn(Mono.just(100L));
        when(attachmentRepository.countByType(AttachmentType.File)).thenReturn(Mono.just(80L));
        when(attachmentRepository.countByType(AttachmentType.Directory)).thenReturn(Mono.just(20L));

        // Subject repository counts
        when(subjectRepository.count()).thenReturn(Mono.just(50L));
        when(subjectRepository.countByType(SubjectType.ANIME)).thenReturn(Mono.just(30L));
        when(subjectRepository.countByType(SubjectType.COMIC)).thenReturn(Mono.just(5L));
        when(subjectRepository.countByType(SubjectType.GAME)).thenReturn(Mono.just(3L));
        when(subjectRepository.countByType(SubjectType.MUSIC)).thenReturn(Mono.just(2L));
        when(subjectRepository.countByType(SubjectType.NOVEL)).thenReturn(Mono.just(4L));
        when(subjectRepository.countByType(SubjectType.REAL)).thenReturn(Mono.just(4L));
        when(subjectRepository.countByType(SubjectType.OTHER)).thenReturn(Mono.just(2L));

        // Subject collection counts
        when(subjectCollectionRepository.count()).thenReturn(Mono.just(40L));
        when(subjectCollectionRepository.countByType(CollectionType.WISH)).thenReturn(Mono.just(10L));
        when(subjectCollectionRepository.countByType(CollectionType.DOING)).thenReturn(Mono.just(15L));
        when(subjectCollectionRepository.countByType(CollectionType.DONE)).thenReturn(Mono.just(10L));
        when(subjectCollectionRepository.countByType(CollectionType.SHELVE)).thenReturn(Mono.just(3L));
        when(subjectCollectionRepository.countByType(CollectionType.DISCARD)).thenReturn(Mono.just(2L));

        // Character and person counts
        when(characterRepository.count()).thenReturn(Mono.just(200L));
        when(personRepository.count()).thenReturn(Mono.just(150L));

        // When
        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);
        Info info = builder.build();

        // Then
        assertNotNull(info.getDetails());
        @SuppressWarnings("unchecked")
        Map<String, Object> detailsMap = (Map<String, Object>) info.getDetails();

        // Verify attachment map
        @SuppressWarnings("unchecked")
        Map<String, Object> attachmentMap = (Map<String, Object>) detailsMap.get("attachment");
        assertNotNull(attachmentMap);
        assertEquals(100L, attachmentMap.get("total"));
        assertEquals(80L, attachmentMap.get("file"));
        assertEquals(20L, attachmentMap.get("folder"));

        // Verify subject map
        @SuppressWarnings("unchecked")
        Map<String, Object> subjectMap = (Map<String, Object>) detailsMap.get("subject");
        assertNotNull(subjectMap);
        assertEquals(50L, subjectMap.get("total"));
        assertEquals(30L, subjectMap.get("anime"));
        assertEquals(5L, subjectMap.get("comic"));
        assertEquals(3L, subjectMap.get("game"));
        assertEquals(2L, subjectMap.get("music"));
        assertEquals(4L, subjectMap.get("novel"));
        assertEquals(4L, subjectMap.get("real"));
        assertEquals(2L, subjectMap.get("other"));

        // Verify subject collection map
        @SuppressWarnings("unchecked")
        Map<String, Object> subjectCollectionMap =
            (Map<String, Object>) detailsMap.get("subjectCollection");
        assertNotNull(subjectCollectionMap);
        assertEquals(40L, subjectCollectionMap.get("total"));
        assertEquals(10L, subjectCollectionMap.get("wish"));
        assertEquals(15L, subjectCollectionMap.get("doing"));
        assertEquals(10L, subjectCollectionMap.get("done"));
        assertEquals(3L, subjectCollectionMap.get("shelve"));
        assertEquals(2L, subjectCollectionMap.get("discard"));

        // Verify character map
        @SuppressWarnings("unchecked")
        Map<String, Object> characterMap = (Map<String, Object>) detailsMap.get("character");
        assertNotNull(characterMap);
        assertEquals(200L, characterMap.get("total"));

        // Verify person map
        @SuppressWarnings("unchecked")
        Map<String, Object> personMap = (Map<String, Object>) detailsMap.get("person");
        assertNotNull(personMap);
        assertEquals(150L, personMap.get("total"));

        // Verify repository methods were called
        verify(attachmentRepository).count();
        verify(attachmentRepository).countByType(AttachmentType.File);
        verify(attachmentRepository).countByType(AttachmentType.Directory);
        verify(subjectRepository).count();
        verify(subjectRepository).countByType(SubjectType.ANIME);
        verify(subjectRepository).countByType(SubjectType.COMIC);
        verify(subjectRepository).countByType(SubjectType.GAME);
        verify(subjectRepository).countByType(SubjectType.MUSIC);
        verify(subjectRepository).countByType(SubjectType.NOVEL);
        verify(subjectRepository).countByType(SubjectType.REAL);
        verify(subjectRepository).countByType(SubjectType.OTHER);
        verify(subjectCollectionRepository).count();
        verify(subjectCollectionRepository).countByType(CollectionType.WISH);
        verify(subjectCollectionRepository).countByType(CollectionType.DOING);
        verify(subjectCollectionRepository).countByType(CollectionType.DONE);
        verify(subjectCollectionRepository).countByType(CollectionType.SHELVE);
        verify(subjectCollectionRepository).countByType(CollectionType.DISCARD);
        verify(characterRepository).count();
        verify(personRepository).count();
    }
}
