package run.ikaros.api.core.binding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.core.subject.Subject;

/** 确认本地扫描预览并创建绑定任务的请求。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LocalScanConfirmRequest {
    /** 已完成预览的目录附件标识。 */
    @JsonProperty("directory_id")
    private UUID directoryId;
    /** 确认时采用的媒体扫描模式。 */
    private LocalMediaMode mode;
    /** 已有条目的标识。 */
    @JsonProperty("subject_id")
    private UUID subjectId;
    /** 要新建的条目。 */
    private Subject subject;
    /** 仅针对待确认扫描项的人工关联结果。 */
    private List<LocalScanAssignment> assignments;

    /**
     * 判断已有条目和新建条目是否恰好选择其一。
     *
     * @return 恰好选择其一时为 true
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "subjectId 与 subject 必须恰好提供一个")
    public boolean isSubjectSelectionValid() {
        return (subjectId == null) != (subject == null);
    }
}
