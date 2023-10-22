package run.ikaros.server.core.actuator;

import static run.ikaros.api.store.enums.CollectionType.DISCARD;
import static run.ikaros.api.store.enums.CollectionType.DOING;
import static run.ikaros.api.store.enums.CollectionType.DONE;
import static run.ikaros.api.store.enums.CollectionType.SHELVE;
import static run.ikaros.api.store.enums.CollectionType.WISH;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.CharacterRepository;
import run.ikaros.server.store.repository.PersonRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@Slf4j
@Component
public class IkarosAppInfoContributor implements InfoContributor {
    private final AttachmentRepository attachmentRepository;
    private final SubjectRepository subjectRepository;
    private final CharacterRepository characterRepository;
    private final PersonRepository personRepository;
    private final SubjectCollectionRepository subjectCollectionRepository;

    /**
     * Construct.
     */
    public IkarosAppInfoContributor(AttachmentRepository attachmentRepository,
                                    SubjectRepository subjectRepository,
                                    CharacterRepository characterRepository,
                                    PersonRepository personRepository,
                                    SubjectCollectionRepository subjectCollectionRepository) {
        this.attachmentRepository = attachmentRepository;
        this.subjectRepository = subjectRepository;
        this.characterRepository = characterRepository;
        this.personRepository = personRepository;
        this.subjectCollectionRepository = subjectCollectionRepository;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> attachmentMap = new HashMap<>();
        attachmentMap.put("total", attachmentRepository.count().block());
        attachmentMap.put("file", attachmentRepository.countByType(AttachmentType.File).block());
        attachmentMap.put("folder",
            attachmentRepository.countByType(AttachmentType.Directory).block());

        Map<String, Object> subjectMap = new HashMap<>();
        subjectMap.put("total", subjectRepository.count().block());
        subjectMap.put("anime", subjectRepository.countByType(SubjectType.ANIME).block());
        subjectMap.put("comic", subjectRepository.countByType(SubjectType.COMIC).block());
        subjectMap.put("game", subjectRepository.countByType(SubjectType.GAME).block());
        subjectMap.put("music", subjectRepository.countByType(SubjectType.MUSIC).block());
        subjectMap.put("novel", subjectRepository.countByType(SubjectType.NOVEL).block());
        subjectMap.put("real", subjectRepository.countByType(SubjectType.REAL).block());
        subjectMap.put("other", subjectRepository.countByType(SubjectType.OTHER).block());

        Map<String, Object> subjectCollectionMap = new HashMap<>();
        subjectCollectionMap.put("total", subjectCollectionRepository.count().block());
        subjectCollectionMap.put("wish", subjectCollectionRepository.countByType(WISH).block());
        subjectCollectionMap.put("doing", subjectCollectionRepository.countByType(DOING).block());
        subjectCollectionMap.put("done", subjectCollectionRepository.countByType(DONE).block());
        subjectCollectionMap.put("shelve", subjectCollectionRepository.countByType(SHELVE).block());
        subjectCollectionMap.put("discard",
            subjectCollectionRepository.countByType(DISCARD).block());

        Map<String, Object> characterMap = new HashMap<>();
        characterMap.put("total", characterRepository.count().block());

        Map<String, Object> personMap = new HashMap<>();
        personMap.put("total", personRepository.count().block());

        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("attachment", attachmentMap);
        detailsMap.put("subject", subjectMap);
        detailsMap.put("subjectCollection", subjectCollectionMap);
        detailsMap.put("character", characterMap);
        detailsMap.put("person", personMap);

        builder.withDetails(detailsMap);
    }
}
