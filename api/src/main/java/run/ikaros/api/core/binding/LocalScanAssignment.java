package run.ikaros.api.core.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

/** 用户对待确认扫描项指定的关联结果. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LocalScanAssignment {
    /** 待确认附件的标识. */
    @JsonProperty("attachment_id")
    private @Nullable UUID attachmentId;
    /** 指定的主资源附件标识；为空时表示解除关联. */
    @JsonProperty("primary_attachment_id")
    private @Nullable UUID primaryAttachmentId;
}
