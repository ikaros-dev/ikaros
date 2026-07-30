package run.ikaros.api.core.attachment;

import java.time.LocalDateTime;
import java.util.UUID;
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
    private UUID id;
    private UUID parentId;
    private AttachmentType type;
    /**
     * HTTP path, format: driver_id://remote_path
     * .
     */
    private String url;
    /**
     * Attachment logic path.
     */
    private String path;
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
    /** 文件系统中的最后修改时间，用于判断驱动附件是否发生变化. */
    private LocalDateTime modifiedTime;
    private Boolean deleted;
    private UUID driverId;
    private String sha1;
}
