package run.ikaros.api.core.tag;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AttachmentTag {
    private Long id;
    private Long attachmentId;
    private String name;
    private Long userId;
    private LocalDateTime createTime;
}
