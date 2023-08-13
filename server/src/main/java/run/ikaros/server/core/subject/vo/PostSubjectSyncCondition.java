package run.ikaros.server.core.subject.vo;

import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.emus.SubjectSyncAction;

@Data
@Builder
public class PostSubjectSyncCondition {
    private Long subjectId;
    private SubjectSyncPlatform platform;
    private String platformId;
    private SubjectSyncAction subjectSyncAction;
}
