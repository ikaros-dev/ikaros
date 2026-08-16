package run.ikaros.api.core.attachment;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AttachmentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Attachment {
    private @Nullable UUID id;
    private @Nullable UUID parentId;
    private @Nullable AttachmentType type;
    /**
     * HTTP path, format: driver_id://remote_path
     * .
     */
    private @Nullable String url;
    /**
     * Attachment logic path.
     */
    private @Nullable String path;
    /**
     * File path in file system.
     */
    private @Nullable String fsPath;
    /**
     * filename with postfix.
     */
    private @Nullable String name;
    private @Nullable Long size;
    private @Nullable LocalDateTime updateTime;
    /** 文件系统中的最后修改时间，用于判断驱动附件是否发生变化. */
    private @Nullable LocalDateTime modifiedTime;
    private @Nullable Boolean deleted;
    private @Nullable UUID driverId;
    private @Nullable String sha1;
}
