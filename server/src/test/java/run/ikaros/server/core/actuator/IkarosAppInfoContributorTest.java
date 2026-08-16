package run.ikaros.server.core.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static run.ikaros.api.core.attachment.AttachmentConst.COVER_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.DOWNLOAD_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.CharacterRepository;
import run.ikaros.server.store.repository.PersonRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@org.jspecify.annotations.NullUnmarked
class IkarosAppInfoContributorTest {

    private AttachmentRepository attachmentRepository;
    private SubjectRepository subjectRepository;
    private CharacterRepository characterRepository;
    private PersonRepository personRepository;
    private SubjectCollectionRepository subjectCollectRep;
    private IkarosAppInfoContributor contributor;

    @BeforeEach
    void setUp() {
        attachmentRepository = mock(AttachmentRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        characterRepository = mock(CharacterRepository.class);
        personRepository = mock(PersonRepository.class);
        subjectCollectRep = mock(SubjectCollectionRepository.class);
        contributor = new IkarosAppInfoContributor(
            attachmentRepository, subjectRepository, characterRepository,
            personRepository, subjectCollectRep);
    }

    @Test
    void contribute_populatesDetails() {
        // Given - mock all attachment repository count methods
        when(attachmentRepository.countKnownFiles()).thenReturn(Mono.just(80L));
        when(attachmentRepository.countKnownFolders(
            ROOT_DIRECTORY_ID, COVER_DIRECTORY_ID, DOWNLOAD_DIRECTORY_ID))
            .thenReturn(Mono.just(20L));

        // Subject repository counts
        when(subjectRepository.countActive()).thenReturn(Mono.just(50L));
        when(subjectRepository.countActiveByType(SubjectType.ANIME)).thenReturn(Mono.just(30L));
        when(subjectRepository.countActiveByType(SubjectType.COMIC)).thenReturn(Mono.just(5L));
        when(subjectRepository.countActiveByType(SubjectType.GAME)).thenReturn(Mono.just(3L));
        when(subjectRepository.countActiveByType(SubjectType.MUSIC)).thenReturn(Mono.just(2L));
        when(subjectRepository.countActiveByType(SubjectType.NOVEL)).thenReturn(Mono.just(4L));
        when(subjectRepository.countActiveByType(SubjectType.REAL)).thenReturn(Mono.just(4L));
        when(subjectRepository.countActiveByType(SubjectType.VIDEO)).thenReturn(Mono.just(6L));
        when(subjectRepository.countActiveByType(SubjectType.OTHER)).thenReturn(Mono.just(2L));

        // Subject collection counts
        when(subjectCollectRep.countActive()).thenReturn(Mono.just(40L));
        when(subjectCollectRep.countActiveByType(CollectionType.WISH)).thenReturn(Mono.just(10L));
        when(subjectCollectRep.countActiveByType(CollectionType.DOING)).thenReturn(Mono.just(15L));
        when(subjectCollectRep.countActiveByType(CollectionType.DONE)).thenReturn(Mono.just(10L));
        when(subjectCollectRep.countActiveByType(CollectionType.SHELVE)).thenReturn(Mono.just(3L));
        when(subjectCollectRep.countActiveByType(CollectionType.DISCARD)).thenReturn(Mono.just(2L));

        // Character and person counts
        when(characterRepository.countActive()).thenReturn(Mono.just(200L));
        when(personRepository.countActive()).thenReturn(Mono.just(150L));

        // When
        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);
        Info info = builder.build();

        // Then
        assertThat(info.getDetails()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> detailsMap = (Map<String, Object>) info.getDetails();

        // Verify attachment map
        @SuppressWarnings("unchecked")
        Map<String, Object> attachmentMap = (Map<String, Object>) detailsMap.get("attachment");
        assertThat(attachmentMap).isNotNull();
        assertThat(attachmentMap.get("file")).isEqualTo(80L);
        assertThat(attachmentMap.get("folder")).isEqualTo(20L);

        // Verify subject map
        @SuppressWarnings("unchecked")
        Map<String, Object> subjectMap = (Map<String, Object>) detailsMap.get("subject");
        assertThat(subjectMap).isNotNull();
        assertThat(subjectMap.get("total")).isEqualTo(50L);
        assertThat(subjectMap.get("anime")).isEqualTo(30L);
        assertThat(subjectMap.get("comic")).isEqualTo(5L);
        assertThat(subjectMap.get("game")).isEqualTo(3L);
        assertThat(subjectMap.get("music")).isEqualTo(2L);
        assertThat(subjectMap.get("novel")).isEqualTo(4L);
        assertThat(subjectMap.get("real")).isEqualTo(4L);
        assertThat(subjectMap.get("video")).isEqualTo(6L);
        assertThat(subjectMap.get("other")).isEqualTo(2L);

        // Verify subject collection map
        @SuppressWarnings("unchecked")
        Map<String, Object> subjectCollectionMap =
            (Map<String, Object>) detailsMap.get("subjectCollection");
        assertThat(subjectCollectionMap).isNotNull();
        assertThat(subjectCollectionMap.get("total")).isEqualTo(40L);
        assertThat(subjectCollectionMap.get("wish")).isEqualTo(10L);
        assertThat(subjectCollectionMap.get("doing")).isEqualTo(15L);
        assertThat(subjectCollectionMap.get("done")).isEqualTo(10L);
        assertThat(subjectCollectionMap.get("shelve")).isEqualTo(3L);
        assertThat(subjectCollectionMap.get("discard")).isEqualTo(2L);

        // Verify character map
        @SuppressWarnings("unchecked")
        Map<String, Object> characterMap = (Map<String, Object>) detailsMap.get("character");
        assertThat(characterMap).isNotNull();
        assertThat(characterMap.get("total")).isEqualTo(200L);

        // Verify person map
        @SuppressWarnings("unchecked")
        Map<String, Object> personMap = (Map<String, Object>) detailsMap.get("person");
        assertThat(personMap).isNotNull();
        assertThat(personMap.get("total")).isEqualTo(150L);

        // Verify repository methods were called
        verify(attachmentRepository).countKnownFiles();
        verify(attachmentRepository).countKnownFolders(
            ROOT_DIRECTORY_ID, COVER_DIRECTORY_ID, DOWNLOAD_DIRECTORY_ID);
        verify(subjectRepository).countActive();
        verify(subjectRepository).countActiveByType(SubjectType.ANIME);
        verify(subjectRepository).countActiveByType(SubjectType.COMIC);
        verify(subjectRepository).countActiveByType(SubjectType.GAME);
        verify(subjectRepository).countActiveByType(SubjectType.MUSIC);
        verify(subjectRepository).countActiveByType(SubjectType.NOVEL);
        verify(subjectRepository).countActiveByType(SubjectType.REAL);
        verify(subjectRepository).countActiveByType(SubjectType.VIDEO);
        verify(subjectRepository).countActiveByType(SubjectType.OTHER);
        verify(subjectCollectRep).countActive();
        verify(subjectCollectRep).countActiveByType(CollectionType.WISH);
        verify(subjectCollectRep).countActiveByType(CollectionType.DOING);
        verify(subjectCollectRep).countActiveByType(CollectionType.DONE);
        verify(subjectCollectRep).countActiveByType(CollectionType.SHELVE);
        verify(subjectCollectRep).countActiveByType(CollectionType.DISCARD);
        verify(characterRepository).countActive();
        verify(personRepository).countActive();
    }
}
