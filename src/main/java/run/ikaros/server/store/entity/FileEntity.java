package run.ikaros.server.store.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.server.store.enums.FilePlace;

/**
 * file entity.
 *
 * @author liguohao
 */
@Data
@Table(name = "file")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FileEntity extends BaseEntity {

    private String url;

    /**
     * filename with postfix.
     */
    private String name;

    private String type;
    private String md5;
    private Long size = -1L;

    private FilePlace place = FilePlace.LOCAL;

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