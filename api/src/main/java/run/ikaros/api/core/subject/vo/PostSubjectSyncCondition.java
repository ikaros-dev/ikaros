package run.ikaros.api.core.subject.vo;

import lombok.Builder;
import lombok.Data;
import run.ikaros.api.core.subject.SubjectSyncAction;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

@Data
@Builder
public class PostSubjectSyncCondition {
    private Long subjectId;
    private SubjectSyncPlatform platform;
    private String platformId;
    private SubjectSyncAction subjectSyncAction;
}
