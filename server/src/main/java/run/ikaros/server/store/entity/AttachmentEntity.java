package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
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
    private Long id;
    @Column("parent_id")
    private Long parentId;
    private AttachmentType type;
    /**
     * HTTP path, format: driver_id://relactive_path
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
}
