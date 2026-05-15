package run.ikaros.api.core.subject.vo;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

@Data
@Builder
public class PostSubjectSyncCondition {
    /**
     * 为空则是拉取创建新的，不为空则是先查询数据库，存在则更新，不存在则新增.
     */
    private UUID subjectId;
    private SubjectSyncPlatform platform;
    private String platformId;
}
