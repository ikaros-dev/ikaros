package cn.liguohao.ikaros.file;

import static cn.liguohao.ikaros.constants.ItemDataConstants.DEFAULT_POSTFIX;

import cn.liguohao.ikaros.common.Assert;
import java.io.File;
import java.time.LocalDateTime;

/**
 * 具体的项数据，条目中具体的项在文件系统里的数据，即传统意义上的文件，如番剧的视频文件等。
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class ItemData {
    /**
     * 数据
     */
    private byte[] datum;

    /**
     * 类型
     */
    private ItemDataType type;

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

    /**
     * 解析剧集的路径，获取对应的项数据实例
     *
     * @param path         剧集的路径
     * @param uploadedTime 项数据上传时间
     * @return 项数据实例
     */
    public static ItemData parseEpisodePath(String path, LocalDateTime uploadedTime) {
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


        return new ItemData()
            .setType(ItemDataType.UNKNOWN)
            .setName(name)
            .setPostfix(postfix)
            .setUploadedTime(uploadedTime);
    }

    /**
     * 构建一个项数据实例
     *
     * @param datum    数据字节数组
     * @param itemName 项数据文件名
     * @return 实例
     */
    public static ItemData buildInstanceByDatum(byte[] datum, String itemName) {
        Assert.isNotNull(datum, itemName);
        Assert.isNotBlank(itemName);

        int lastDotIndex = itemName.lastIndexOf(".");

        String postfix;
        String name;

        if (lastDotIndex > 0) {
            postfix = itemName.substring(lastDotIndex);
            name = itemName.substring(0, lastDotIndex);
        } else {
            name = itemName;
            postfix = DEFAULT_POSTFIX;
        }


        return new ItemData()
            .setType(ItemDataType.UNKNOWN)
            .setName(name)
            .setPostfix(postfix)
            .setDatum(datum);
    }


    /**
     * 下载前校验是否少传了必要的参数
     *
     * @return this
     */
    public ItemData checkoutBeforeDownload() {
        Assert.isNotNull(type, name, postfix, uploadedTime);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    /**
     * 上传前校验是否少传了必要的参数
     *
     * @return this
     */
    public ItemData checkoutBeforeUpload() {
        Assert.isNotNull(name, postfix, datum);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    /**
     * 删除前校验是否少传了必要的参数
     *
     * @return this
     */
    public ItemData checkoutBeforeDelete() {
        Assert.isNotNull(name, postfix, uploadedTime);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    public byte[] datum() {
        return datum;
    }

    public ItemData setDatum(byte[] datum) {
        this.datum = datum;
        return this;
    }

    public ItemDataType type() {
        return type;
    }

    public ItemData setType(ItemDataType type) {
        this.type = type;
        return this;
    }

    public String name() {
        return name;
    }

    public ItemData setName(String name) {
        this.name = name;
        return this;
    }

    public String postfix() {
        return postfix;
    }

    public ItemData setPostfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    public LocalDateTime uploadedTime() {
        return uploadedTime;
    }

    public ItemData setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
        return this;
    }

    public String uploadedPath() {
        return uploadedPath;
    }

    public ItemData setUploadedPath(String uploadedPath) {
        this.uploadedPath = uploadedPath;
        return this;
    }
}
