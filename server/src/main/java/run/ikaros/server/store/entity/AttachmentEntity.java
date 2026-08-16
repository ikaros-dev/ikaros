package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.AttachmentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "attachment")
public class AttachmentEntity {
    @Id
    private UUID id;
    @Column("parent_id")
    private @Nullable UUID parentId;
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
    @Column("fs_path")
    private String fsPath;
    /**
     * filename with postfix.
     */
    private String name;
    private Long size;
    @Column("update_time")
    private LocalDateTime updateTime;
    /** 文件系统中的最后修改时间，用于判断驱动附件是否发生变化. */
    @Column("modified_time")
    private LocalDateTime modifiedTime;
    private Boolean deleted;
    @Column("driver_id")
    private @Nullable UUID driverId;
    private String sha1;
}
