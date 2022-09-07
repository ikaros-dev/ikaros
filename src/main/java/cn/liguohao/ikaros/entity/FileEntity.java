package cn.liguohao.ikaros.entity;

import cn.liguohao.ikaros.file.IkarosFile;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author liguohao
 */
@Entity
@Table(name = "file")
public class FileEntity extends BaseEntity {

    private String name;
    private String postfix;
    private IkarosFile.Type type;
    private String sha256;
    private String md5;
    private Integer size;
    private String location;


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

    public String getSha256() {
        return sha256;
    }

    public FileEntity setSha256(String sha256) {
        this.sha256 = sha256;
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


}