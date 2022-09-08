package cn.liguohao.ikaros.file;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.FileKit;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.SystemVarKit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 条目数据处理器的 本地文件系统实现。数据存放在当前程序的[upload/yyyy/MM/dd/HH/]目录下。
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class LocalIkarosFileHandler implements IkarosFileHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalIkarosFileHandler.class);

    private static final String BASE_UPLOAD_DIR_NAME = "upload";

    private static final String BASE_UPLOAD_DIR_PATH
        = SystemVarKit.getCurrentAppDirPath() + File.separator + BASE_UPLOAD_DIR_NAME;


    @Override
    public IkarosFileOperateResult upload(IkarosFile ikarosFile) throws IOException {
        Assert.isNotNull(ikarosFile);
        ikarosFile.checkoutBeforeUpload();

        LocalDateTime oldUploadedTime = ikarosFile.getUploadedTime();
        ikarosFile.setUploadedTime(LocalDateTime.now());


        // 相同文件不重复上传
        final String oldLocation = ikarosFile.getOldLocation();
        final String md5 = ikarosFile.getMd5();
        final String sha256 = ikarosFile.getSha256();
        boolean isSameFile = false;
        if (Strings.isNotBlank(oldLocation)) {
            File oldFile = new File(oldLocation);
            if (oldFile.exists()) {
                try (FileInputStream fileInputStream = new FileInputStream(oldFile)) {
                    byte[] bytes = fileInputStream.readAllBytes();

                    if (!isSameFile && Strings.isNotBlank(md5)) {
                        final String oldMd5 = FileKit.checksum2Str(bytes, FileKit.Hash.MD5);
                        if (md5.equals(oldMd5)) {
                            isSameFile = true;
                        }
                    }

                    if (!isSameFile && Strings.isNotBlank(sha256)) {
                        final String oldSha256 = FileKit.checksum2Str(bytes, FileKit.Hash.SHA256);
                        if (sha256.equals(oldSha256)) {
                            isSameFile = true;
                        }
                    }

                }
            }
        }

        try {
            if (isSameFile) {
                ikarosFile.setUploadedPath(oldLocation);
                LOGGER.debug("repeated ikaros file, do not upload, path={}", oldLocation);
            } else {
                String subjectDataFilePath = buildSubjectDataFilePath(ikarosFile);
                Files.write(Path.of(new File(subjectDataFilePath).toURI()), ikarosFile.getBytes());
                ikarosFile.setUploadedPath(subjectDataFilePath);
                LOGGER.debug("upload ikaros file data success, path={}", subjectDataFilePath);
            }

        } catch (IOException e) {
            ikarosFile.setUploadedTime(oldUploadedTime);
            String msg = "operate file fail for uploadedPath=" + ikarosFile.getUploadedPath();
            LOGGER.error(msg, e);
            return IkarosFileOperateResult.ofUploadFail(msg + ", exception: ", e);
        }

        return IkarosFileOperateResult.ofOk(ikarosFile);
    }

    /**
     * @param ikarosFile 条目数据实例
     * @return 条目数据的文件路径，格式：[upload/yyyy/MM/dd/HH/随机生成的UUID.postfix]
     */
    public String buildSubjectDataFilePath(IkarosFile ikarosFile) {
        return buildLocationDirAndReturnPath(ikarosFile.getUploadedTime())
            + File.separator + UUID.randomUUID().toString().replace("-", "")
            + (('.' == ikarosFile.getPostfix().charAt(0))
            ? ikarosFile.getPostfix() : "." + ikarosFile.getPostfix());
    }

    /**
     * @param uploadedTime 条目数据上传的时间
     * @return 基础的上传目录路径，格式：[upload/yyyy/MM/dd/HH]
     */
    public String buildLocationDirAndReturnPath(LocalDateTime uploadedTime) {
        String locationDirPath = BASE_UPLOAD_DIR_PATH
            + File.separator + uploadedTime.getYear()
            + File.separator + uploadedTime.getMonthValue()
            + File.separator + uploadedTime.getDayOfMonth()
            + File.separator + uploadedTime.getHour();

        File locationDir = new File(locationDirPath);
        if (!locationDir.exists()) {
            locationDir.mkdirs();
        }
        return locationDirPath;
    }

    @Override
    public IkarosFileOperateResult download(IkarosFile ikarosFile) {
        Assert.isNotNull(ikarosFile);
        ikarosFile.checkoutBeforeDownload();
        Assert.isNotNull(ikarosFile.getUploadedTime());

        String itemDataFilePath = ikarosFile.getUploadedPath();
        if (itemDataFilePath == null || "".equals(itemDataFilePath)) {
            itemDataFilePath = buildSubjectDataFilePath(ikarosFile);
        }

        File subjectDataFile = new File(itemDataFilePath);
        if (!subjectDataFile.exists()) {
            return IkarosFileOperateResult
                .ofNotFound("current subject data not found, itemDataFilePath="
                    + itemDataFilePath);
        }

        try {
            byte[] datum = Files.readAllBytes(Path.of(subjectDataFile.toURI()));
            ikarosFile.setBytes(datum);
            LOGGER.debug("download subject data success, path={}", itemDataFilePath);
        } catch (IOException e) {
            String msg = "download subject data fail for path=" + itemDataFilePath;
            LOGGER.error(msg, e);
            return IkarosFileOperateResult.ofDownloadFail(msg + ", exception: ", e);
        }

        return IkarosFileOperateResult.ofOk(ikarosFile);
    }

    @Override
    public IkarosFileOperateResult delete(IkarosFile ikarosFile) {
        Assert.isNotNull(ikarosFile);
        ikarosFile.checkoutBeforeDelete();

        String subjectDataFilePath = buildSubjectDataFilePath(ikarosFile);

        return delete(subjectDataFilePath);
    }

    @Override
    public IkarosFileOperateResult delete(String uploadedPath) {
        Assert.isNotBlank(uploadedPath);
        try {
            File file = new File(uploadedPath);
            if (!file.exists()) {
                return IkarosFileOperateResult.ofOk(
                    "file not exist, do nothing. file path: " + uploadedPath);
            }
            boolean deleteSuccess = file.delete();
            if (!deleteSuccess) {
                return IkarosFileOperateResult.ofDeleteFail(
                    "delete file fail for path=" + uploadedPath, null);
            }
            LOGGER.debug("delete file success for path={}", file);
            return IkarosFileOperateResult.ofOk();
        } catch (Exception e) {
            String msg = "delete file fail for path=" + uploadedPath;
            LOGGER.error(msg, e);
            return IkarosFileOperateResult.ofDeleteFail(msg + ", exception: ", e);
        }
    }

    @Override
    public boolean exist(String uploadedPath) {
        File itemDataFile = new File(uploadedPath);
        return itemDataFile.exists();
    }
}
