package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.FilePlace;

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
@EqualsAndHashCode(callSuper = true)
public class FileEntity extends BaseEntity {
    @Column("folder_id")
    private Long folderId;
    private String url;

    /**
     * filename with postfix.
     */
    private String name;

    private String type;
    private String md5;
    private Long size;

    private FilePlace place;

    /**
     * original path in file system.
     */
    @Column("original_path")
    private String originalPath;

    /**
     * file original name before upload.
     */
    @Column("original_name")
    private String originalName;

}