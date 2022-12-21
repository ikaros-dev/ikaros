package run.ikaros.server.entity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.FileType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * @author liguohao
 */
@Data
@Entity
@Table(name = "file")
@EqualsAndHashCode(callSuper = true)
public class FileEntity extends BaseEntity {

    @Column(nullable = false)
    private String url;

    /**
     * 完整带后缀文件名称
     */
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private FileType type = FileType.UNKNOWN;
    private String md5;
    @Column(nullable = false)
    private Integer size = -1;

    @Enumerated(EnumType.STRING)
    private FilePlace place = FilePlace.LOCAL;

    /**
     * 文件在下载目录的绝对路径，目的是为了能够还原文件的结构
     */
    @Column(name = "original_path")
    private String originalPath;
    /**
     * 文件在下载目录的目录名称，目的是为了能够还原文件的结构
     */
    @Column(name = "dir_name")
    private String dirName;

}