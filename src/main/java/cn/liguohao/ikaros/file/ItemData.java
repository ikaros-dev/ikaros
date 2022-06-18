package cn.liguohao.ikaros.file;

import cn.liguohao.ikaros.common.Assert;
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
     * 获取一个上传前用的实例
     *
     * @param name    文件名
     * @param postfix 文件后缀
     * @param datum   数据
     * @return 实例
     */
    public static ItemData ofUploadInstance(
        String name, String postfix, byte[] datum) {
        Assert.isNotNull(name, postfix, datum);
        Assert.isNotBlank(name, postfix);
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
        Assert.isNotNull(type, name, postfix, datum);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    /**
     * 删除前校验是否少传了必要的参数
     *
     * @return this
     */
    public ItemData checkoutBeforeDelete() {
        Assert.isNotNull(type, name, postfix, uploadedTime);
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
}
