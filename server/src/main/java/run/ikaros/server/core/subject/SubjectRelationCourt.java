package run.ikaros.server.core.subject;

import java.util.HashMap;
import java.util.Map;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.api.store.enums.SubjectType;

public class SubjectRelationCourt {

    public static final Map<SubjectType, SubjectRelationType> subjectRelationTypeMap =
        new HashMap<>();

    static {
        subjectRelationTypeMap.put(SubjectType.ANIME, SubjectRelationType.ANIME);
        subjectRelationTypeMap.put(SubjectType.COMIC, SubjectRelationType.COMIC);
        subjectRelationTypeMap.put(SubjectType.GAME, SubjectRelationType.GAME);
        subjectRelationTypeMap.put(SubjectType.MUSIC, SubjectRelationType.MUSIC);
        subjectRelationTypeMap.put(SubjectType.NOVEL, SubjectRelationType.NOVEL);
        subjectRelationTypeMap.put(SubjectType.REAL, SubjectRelationType.REAL);
        subjectRelationTypeMap.put(SubjectType.OTHER, SubjectRelationType.OTHER);
    }

    /**
     * Judge subject relation.
     */
    public static SubjectRelationType judge(SubjectType masterType,
                                            SubjectRelationType relationType) {
        switch (relationType) {
            case BEFORE -> {
                return SubjectRelationType.AFTER;
            }
            case AFTER -> {
                return SubjectRelationType.BEFORE;
            }
            case SAME_WORLDVIEW -> {
                return SubjectRelationType.SAME_WORLDVIEW;
            }
            default -> {
                return subjectRelationTypeMap.get(masterType);
            }
        }
    }

}
