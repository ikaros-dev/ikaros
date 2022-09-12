package cn.liguohao.ikaros.model.file;

import static cn.liguohao.ikaros.common.constants.IkarosFileConstants.DEFAULT_POSTFIX;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.kit.FileKit;
import java.io.File;
import java.time.LocalDateTime;

/**
 * 具体的项数据，条目中具体的项在文件系统里的数据，即传统意义上的文件，如番剧的视频文件等。
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class IkarosFile {
    /**
     * 数据
     */
    private byte[] bytes;

    /**
     * 类型
     */
    private Type type;

    /**
     * 名称，不带后缀
     */
    private String name;

    /**
     * 后缀，比如.txt .mp4等
     */
    private String postfix;

    /**
     * 上传时间
     */
    private LocalDateTime uploadedTime;
    /**
     * 上传后的路径
     */
    private String uploadedPath;

    private String sha256;
    private String md5;
    private String oldLocation;

    public enum Type {

        /**
         * 图片
         */
        IMAGE,

        /**
         * 视频
         */
        VIDEO,

        /**
         * 文档
         */
        DOCUMENT,

        /**
         * 音频
         */
        VOICE,

        /**
         * 未知
         */
        UNKNOWN,
        ;
    }

    /**
     * 解析剧集的路径，获取对应的项数据实例
     *
     * @param path         剧集的路径
     * @param uploadedTime 项数据上传时间
     * @return 项数据实例
     */
    public static IkarosFile parseEpisodePath(String path, LocalDateTime uploadedTime) {
        Assert.isNotNull(path, uploadedTime);
        Assert.isNotBlank(path);

        int lastDotIndex = path.lastIndexOf(".");
        int lastSeparatorIndex = path.lastIndexOf(File.separator);

        String postfix;
        String name;

        if (lastDotIndex > 0 && lastSeparatorIndex > 0) {
            postfix = path.substring(lastDotIndex);
            name = path.substring(lastSeparatorIndex, lastDotIndex);
        } else {
            postfix = DEFAULT_POSTFIX;
            name = path.length() <= 160 ? path : path.substring(0, 160);
        }


        return new IkarosFile()
            .setType(Type.UNKNOWN)
            .setName(name)
            .setPostfix(postfix)
            .setUploadedTime(uploadedTime);
    }

    public static IkarosFile build(String originalFilename, byte[] bytes) {
        Assert.isNotNull(bytes, originalFilename);
        Assert.isNotBlank(originalFilename);

        int lastDotIndex = originalFilename.lastIndexOf(".");

        String postfix;
        String name;

        if (lastDotIndex > 0) {
            postfix = originalFilename.substring(lastDotIndex + 1);
            name = originalFilename.substring(0, lastDotIndex);
        } else {
            name = originalFilename;
            postfix = DEFAULT_POSTFIX;
        }


        return new IkarosFile()
            .setName(name)
            .setPostfix(postfix)
            .setBytes(bytes);
    }


    /**
     * 下载前校验是否少传了必要的参数
     *
     * @return this
     */
    public IkarosFile checkoutBeforeDownload() {
        Assert.isNotNull(type, name, postfix, uploadedTime);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    /**
     * 上传前校验是否少传了必要的参数
     *
     * @return this
     */
    public IkarosFile checkoutBeforeUpload() {
        Assert.isNotNull(name, postfix, bytes);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    /**
     * 删除前校验是否少传了必要的参数
     *
     * @return this
     */
    public IkarosFile checkoutBeforeDelete() {
        Assert.isNotNull(name, postfix, uploadedTime);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public IkarosFile setBytes(byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

    public Type getType() {
        return type;
    }

    public IkarosFile setType(Type type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public IkarosFile setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostfix() {
        return postfix;
    }

    public IkarosFile setPostfix(String postfix) {
        this.postfix = postfix;
        this.type = FileKit.parseTypeByPostfix(postfix);
        return this;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public IkarosFile setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
        return this;
    }

    public String getUploadedPath() {
        return uploadedPath;
    }

    public IkarosFile setUploadedPath(String uploadedPath) {
        this.uploadedPath = uploadedPath;
        return this;
    }

    public String getSha256() {
        return sha256;
    }

    public IkarosFile setSha256(String sha256) {
        this.sha256 = sha256;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public IkarosFile setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public String getOldLocation() {
        return oldLocation;
    }

    public IkarosFile setOldLocation(String oldLocation) {
        this.oldLocation = oldLocation;
        return this;
    }
}
