package run.ikaros.server.core.actuator;

import static run.ikaros.api.core.attachment.AttachmentConst.COVER_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.DOWNLOAD_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;
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
        attachmentMap.put("file", attachmentRepository.countKnownFiles().block());
        attachmentMap.put("folder", attachmentRepository.countKnownFolders(
            ROOT_DIRECTORY_ID, COVER_DIRECTORY_ID, DOWNLOAD_DIRECTORY_ID).block());

        Map<String, Object> subjectMap = new HashMap<>();
        subjectMap.put("total", subjectRepository.countActive().block());
        subjectMap.put("anime", subjectRepository.countActiveByType(SubjectType.ANIME).block());
        subjectMap.put("comic", subjectRepository.countActiveByType(SubjectType.COMIC).block());
        subjectMap.put("game", subjectRepository.countActiveByType(SubjectType.GAME).block());
        subjectMap.put("music", subjectRepository.countActiveByType(SubjectType.MUSIC).block());
        subjectMap.put("novel", subjectRepository.countActiveByType(SubjectType.NOVEL).block());
        subjectMap.put("real", subjectRepository.countActiveByType(SubjectType.REAL).block());
        subjectMap.put("video", subjectRepository.countActiveByType(SubjectType.VIDEO).block());
        subjectMap.put("other", subjectRepository.countActiveByType(SubjectType.OTHER).block());

        Map<String, Object> subjectCollectionMap = new HashMap<>();
        subjectCollectionMap.put("total", subjectCollectionRepository.countActive().block());
        subjectCollectionMap.put("wish", subjectCollectionRepository.countActiveByType(WISH).block());
        subjectCollectionMap.put("doing", subjectCollectionRepository.countActiveByType(DOING).block());
        subjectCollectionMap.put("done", subjectCollectionRepository.countActiveByType(DONE).block());
        subjectCollectionMap.put("shelve", subjectCollectionRepository.countActiveByType(SHELVE).block());
        subjectCollectionMap.put("discard",
            subjectCollectionRepository.countActiveByType(DISCARD).block());

        Map<String, Object> characterMap = new HashMap<>();
        characterMap.put("total", characterRepository.countActive().block());

        Map<String, Object> personMap = new HashMap<>();
        personMap.put("total", personRepository.countActive().block());

        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("attachment", attachmentMap);
        detailsMap.put("subject", subjectMap);
        detailsMap.put("subjectCollection", subjectCollectionMap);
        detailsMap.put("character", characterMap);
        detailsMap.put("person", personMap);

        builder.withDetails(detailsMap);
    }
}
