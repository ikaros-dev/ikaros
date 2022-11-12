package run.ikaros.server.entity;

import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.FileType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * @author liguohao
 */
@Entity
@Table(name = "file")
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


    public String getName() {
        return name;
    }

    public FileEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public FileEntity setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public FileEntity setSize(Integer size) {
        this.size = size;
        return this;
    }

    public FileType getType() {
        return type;
    }

    public FileEntity setType(FileType type) {
        this.type = type;
        return this;
    }

    public FilePlace getPlace() {
        return place;
    }

    public FileEntity setPlace(FilePlace place) {
        this.place = place;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public FileEntity setUrl(String url) {
        this.url = url;
        return this;
    }
}