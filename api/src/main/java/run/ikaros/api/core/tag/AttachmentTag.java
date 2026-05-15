package run.ikaros.api.core.tag;

import java.time.LocalDateTime;
import java.util.UUID;
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
    private UUID id;
    private UUID attachmentId;
    private String name;
    private UUID userId;
    private LocalDateTime createTime;
}
