package cn.liguohao.ikaros.model.binary;

import java.time.LocalDateTime;

/**
 * @author guohao
 * @date 2022/10/21
 */
public class Binary {
    /**
     * 数据
     */
    private byte[] bytes;
    /**
     * 原始名称，带后缀
     */
    private String name;
    private LocalDateTime uploadedTime;
    /**
     * 上传后的路径
     */
    private String url;
    private String md5;
    private String oldUrl;
    private BinaryPlace place = BinaryPlace.LOCAL;

    public byte[] getBytes() {
        return bytes;
    }

    public Binary setBytes(byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

    public String getName() {
        return name;
    }

    public Binary setName(String name) {
        this.name = name;
        return this;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public Binary setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Binary setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public Binary setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public String getOldUrl() {
        return oldUrl;
    }

    public Binary setOldUrl(String oldUrl) {
        this.oldUrl = oldUrl;
        return this;
    }

    public BinaryPlace getPlace() {
        return place;
    }

    public Binary setPlace(BinaryPlace place) {
        this.place = place;
        return this;
    }
}
