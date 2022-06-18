package cn.liguohao.ikaros.subject;

import cn.liguohao.ikaros.common.Assert;
import java.time.LocalDateTime;

/**
 * 条目数据，条目在文件系统里的数据，即传统意义上的文件，如番剧的视频文件等。
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class SubjectData {
    /**
     * 数据
     */
    private byte[] datum;

    /**
     * 类型
     */
    private SubjectDataType type;

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
     * 下载前校验是否少传了必要的参数
     *
     * @return this
     */
    public SubjectData checkoutBeforeDownload() {
        Assert.isNotNull(type, name, postfix, uploadedTime);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    /**
     * 上传前校验是否少传了必要的参数
     *
     * @return this
     */
    public SubjectData checkoutBeforeUpload() {
        Assert.isNotNull(type, name, postfix, datum);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    /**
     * 删除前校验是否少传了必要的参数
     *
     * @return this
     */
    public SubjectData checkoutBeforeDelete() {
        Assert.isNotNull(type, name, postfix, uploadedTime);
        Assert.isNotBlank(name, postfix);
        return this;
    }

    public byte[] datum() {
        return datum;
    }

    public SubjectData setDatum(byte[] datum) {
        this.datum = datum;
        return this;
    }

    public SubjectDataType type() {
        return type;
    }

    public SubjectData setType(SubjectDataType type) {
        this.type = type;
        return this;
    }

    public String name() {
        return name;
    }

    public SubjectData setName(String name) {
        this.name = name;
        return this;
    }

    public String postfix() {
        return postfix;
    }

    public SubjectData setPostfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    public LocalDateTime uploadedTime() {
        return uploadedTime;
    }

    public SubjectData setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
        return this;
    }
}
