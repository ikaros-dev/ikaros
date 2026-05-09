package run.ikaros.server.core.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
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

    @Test
    void shouldContributeAppInfo() {
        AttachmentRepository attachmentRepository = mock(AttachmentRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        SubjectCollectionRepository subjectCollectionRepository =
            mock(SubjectCollectionRepository.class);

        when(attachmentRepository.count()).thenReturn(Mono.just(10L));
        when(attachmentRepository.countByType(AttachmentType.File)).thenReturn(Mono.just(7L));
        when(attachmentRepository.countByType(AttachmentType.Directory)).thenReturn(Mono.just(3L));

        when(subjectRepository.count()).thenReturn(Mono.just(5L));
        when(subjectRepository.countByType(SubjectType.ANIME)).thenReturn(Mono.just(2L));
        when(subjectRepository.countByType(SubjectType.COMIC)).thenReturn(Mono.just(1L));
        when(subjectRepository.countByType(SubjectType.GAME)).thenReturn(Mono.just(1L));
        when(subjectRepository.countByType(SubjectType.MUSIC)).thenReturn(Mono.just(0L));
        when(subjectRepository.countByType(SubjectType.NOVEL)).thenReturn(Mono.just(0L));
        when(subjectRepository.countByType(SubjectType.REAL)).thenReturn(Mono.just(1L));
        when(subjectRepository.countByType(SubjectType.OTHER)).thenReturn(Mono.just(0L));

        when(subjectCollectionRepository.count()).thenReturn(Mono.just(8L));
        when(subjectCollectionRepository.countByType(CollectionType.WISH))
            .thenReturn(Mono.just(2L));
        when(subjectCollectionRepository.countByType(CollectionType.DOING))
            .thenReturn(Mono.just(3L));
        when(subjectCollectionRepository.countByType(CollectionType.DONE))
            .thenReturn(Mono.just(2L));
        when(subjectCollectionRepository.countByType(CollectionType.SHELVE))
            .thenReturn(Mono.just(1L));
        when(subjectCollectionRepository.countByType(CollectionType.DISCARD))
            .thenReturn(Mono.just(0L));

        when(characterRepository.count()).thenReturn(Mono.just(15L));
        when(personRepository.count()).thenReturn(Mono.just(12L));

        IkarosAppInfoContributor contributor = new IkarosAppInfoContributor(
            attachmentRepository, subjectRepository, characterRepository,
            personRepository, subjectCollectionRepository);

        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);
        Info info = builder.build();

        Map<String, Object> details = info.getDetails();
        assertThat(details).isNotNull();
        assertThat(details).containsKeys("attachment", "subject", "subjectCollection",
            "character", "person");

        @SuppressWarnings("unchecked")
        Map<String, Object> attachmentMap = (Map<String, Object>) details.get("attachment");
        assertThat(attachmentMap.get("total")).isEqualTo(10L);
        assertThat(attachmentMap.get("file")).isEqualTo(7L);
        assertThat(attachmentMap.get("folder")).isEqualTo(3L);

        @SuppressWarnings("unchecked")
        Map<String, Object> subjectMap = (Map<String, Object>) details.get("subject");
        assertThat(subjectMap.get("total")).isEqualTo(5L);
        assertThat(subjectMap.get("anime")).isEqualTo(2L);
        assertThat(subjectMap.get("comic")).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        Map<String, Object> collectionMap =
            (Map<String, Object>) details.get("subjectCollection");
        assertThat(collectionMap.get("total")).isEqualTo(8L);
        assertThat(collectionMap.get("wish")).isEqualTo(2L);
        assertThat(collectionMap.get("doing")).isEqualTo(3L);
        assertThat(collectionMap.get("done")).isEqualTo(2L);

        @SuppressWarnings("unchecked")
        Map<String, Object> characterMap = (Map<String, Object>) details.get("character");
        assertThat(characterMap.get("total")).isEqualTo(15L);

        @SuppressWarnings("unchecked")
        Map<String, Object> personMap = (Map<String, Object>) details.get("person");
        assertThat(personMap.get("total")).isEqualTo(12L);
    }
}
