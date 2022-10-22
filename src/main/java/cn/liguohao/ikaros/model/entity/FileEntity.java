package cn.liguohao.ikaros.model.entity;

import cn.liguohao.ikaros.model.file.IkarosFile;
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

    private String name;
    private String postfix;
    @Enumerated(EnumType.STRING)
    private IkarosFile.Type type;
    private String md5;
    private Integer size;
    /**
     * 在文件系统的路径
     */
    private String location;
    /**
     * 网络访问的路径
     */
    private String url;
    private IkarosFile.Place place;


    public String getName() {
        return name;
    }

    public FileEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostfix() {
        return postfix;
    }

    public FileEntity setPostfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    public IkarosFile.Type getType() {
        return type;
    }

    public FileEntity setType(IkarosFile.Type type) {
        this.type = type;
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

    public String getLocation() {
        return location;
    }

    public FileEntity setLocation(String location) {
        this.location = location;
        return this;
    }

    public IkarosFile.Place getPlace() {
        return place;
    }

    public FileEntity setPlace(IkarosFile.Place place) {
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