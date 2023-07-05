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
import run.ikaros.api.store.enums.FileType;

/**
 * file entity.
 *
 * @author liguohao
 */
@Data
@Table(name = "file")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileEntity {
    @Id
    private Long id;

    @Column("folder_id")
    private Long folderId;
    private String url;
    /**
     * File path in file system.
     */
    @Column("fs_path")
    private String fsPath;
    /**
     * filename with postfix.
     */
    private String name;
    private String md5;
    @Column("aes_key")
    private String aesKey;
    private Long size;
    private FileType type;
    @Column("can_read")
    private Boolean canRead;

    @Column("create_time")
    private LocalDateTime createTime;
}