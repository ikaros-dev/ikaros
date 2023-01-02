package run.ikaros.server.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.store.enums.FilePlace;
import run.ikaros.server.store.enums.FileType;

/**
 * file entity.
 *
 * @author liguohao
 */
@Data
@Entity
@Table(name = "file")
@EqualsAndHashCode(callSuper = true)
public class FileEntity extends BaseEntity {

    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Column(nullable = false)
    private String url;

    /**
     * filename with postfix.
     */
    @Column(nullable = false)
    private String name;


    @Enumerated(EnumType.STRING)
    private FileType type = FileType.UNKNOWN;
    private String md5;
    private Long size = -1L;

    @Enumerated(EnumType.STRING)
    private FilePlace place = FilePlace.LOCAL;

    /**
     * original path in file system.
     */
    @Column(name = "original_path")
    private String originalPath;

    /**
     * file original name before upload.
     */
    @Column(name = "original_name")
    private String originalName;

}