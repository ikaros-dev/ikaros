package run.ikaros.server.file;


import org.aspectj.apache.bcel.generic.Type;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.FileType;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.constants.FileConst;
import run.ikaros.server.utils.FileUtils;
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
    private FileType type;

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
    private FilePlace place;

    public enum Place {
        LOCAL(1);

        private int value;

        Place(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * PS: 大小写不敏感 例子：images/png
     *
     * @return 文件类型 + / + 文件后缀名(不带.)
     */
    public String getTypeStr() {
        return this.type.name() + "/" + this.postfix;
    }


    /**
     * 解析剧集的路径，获取对应的项数据实例
     *
     * @param path         剧集的路径
     * @param uploadedTime 项数据上传时间
     * @return 项数据实例
     */
    public static IkarosFile parseEpisodePath(String path, LocalDateTime uploadedTime) {
        AssertUtils.notNull(uploadedTime, "'uploadedTime' must not be null");
        AssertUtils.notBlank(path, "'path' must not be blank");

        int lastDotIndex = path.lastIndexOf(".");
        int lastSeparatorIndex = path.lastIndexOf(File.separator);

        String postfix;
        String name;

        if (lastDotIndex > 0 && lastSeparatorIndex > 0) {
            postfix = path.substring(lastDotIndex);
            name = path.substring(lastSeparatorIndex, lastDotIndex);
        } else {
            postfix = FileConst.IkarosFile.DEFAULT_POSTFIX;
            name = path.length() <= 160 ? path : path.substring(0, 160);
        }


        return new IkarosFile()
            .setType(FileType.UNKNOWN)
            .setName(name)
            .setPostfix(postfix)
            .setUploadedTime(uploadedTime);
    }

    public static IkarosFile build(String originalFilename, byte[] bytes) {
        AssertUtils.notNull(bytes, "'bytes' must not be null");
        AssertUtils.notBlank(originalFilename, "'originalFilename' must not be blank");

        int lastDotIndex = originalFilename.lastIndexOf(".");

        String postfix;
        String name;

        if (lastDotIndex > 0) {
            postfix = originalFilename.substring(lastDotIndex + 1);
            name = originalFilename.substring(0, lastDotIndex);
        } else {
            name = originalFilename;
            postfix = FileConst.IkarosFile.DEFAULT_POSTFIX;
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
        AssertUtils.notNull(type, "'type' must not be null");
        AssertUtils.notNull(uploadedTime, "'uploadedTime' must not be null");
        AssertUtils.notBlank(name, "'name' must not be blank");
        AssertUtils.notBlank(postfix, "'postfix' must not be blank");
        return this;
    }

    /**
     * 上传前校验是否少传了必要的参数
     *
     * @return this
     */
    public IkarosFile checkoutBeforeUpload() {
        AssertUtils.notNull(bytes, "'bytes' must not be null");
        AssertUtils.notBlank(name, "'name' must not be blank");
        AssertUtils.notBlank(postfix, "'postfix' must not be blank");
        return this;
    }

    /**
     * 删除前校验是否少传了必要的参数
     *
     * @return this
     */
    public IkarosFile checkoutBeforeDelete() {
        AssertUtils.notNull(uploadedTime, "'uploadedTime' must not be null");
        AssertUtils.notBlank(name, "'name' must not be blank");
        AssertUtils.notBlank(postfix, "'postfix' must not be blank");
        return this;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public IkarosFile setBytes(byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

    public FileType getType() {
        return type;
    }

    public IkarosFile setType(FileType type) {
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
        return this;
    }

    public IkarosFile setPlace(FilePlace place) {
        this.place = place;
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

    public FilePlace getPlace() {
        return place;
    }
}
