package run.ikaros.api.core.attachment;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.AttachmentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Attachment {
    private Long id;
    private Long parentId;
    private AttachmentType type;
    /**
     * HTTP path.
     */
    private String url;
    /**
     * File path in file system.
     */
    private String fsPath;
    /**
     * filename with postfix.
     */
    private String name;
    private Long size;
    private LocalDateTime updateTime;
}
