package cn.liguohao.ikaros.persistence.incompact.file;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.SystemVarKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 条目数据处理器的 本地文件系统实现。数据存放在当前程序的[upload/yyyy/MM/dd/HH/]目录下。
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class LocalFileDataHandler implements FileDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileDataHandler.class);

    private static final String BASE_UPLOAD_DIR_NAME = "upload";

    private static final String BASE_UPLOAD_DIR_PATH
        = SystemVarKit.getCurrentAppDirPath() + File.separator + BASE_UPLOAD_DIR_NAME;


    @Override
    public FileDataOperateResult upload(FileData fileData) {
        Assert.isNotNull(fileData);
        fileData.checkoutBeforeUpload();

        LocalDateTime oldUploadedTime = fileData.uploadedTime();
        fileData.setUploadedTime(LocalDateTime.now());

        String subjectDataFilePath = buildSubjectDataFilePath(fileData);

        try {
            Files.write(Path.of(new File(subjectDataFilePath).toURI()), fileData.datum());
            fileData.setUploadedPath(subjectDataFilePath);
            LOGGER.debug("upload subject data success, path={}", subjectDataFilePath);
        } catch (IOException e) {
            fileData.setUploadedTime(oldUploadedTime);
            String msg = "operate file fail for subjectDataFilePath=" + subjectDataFilePath;
            LOGGER.error(msg, e);
            return FileDataOperateResult.ofUploadFail(msg + ", exception: ", e);
        }

        return FileDataOperateResult.ofOk(fileData);
    }

    /**
     * @param fileData 条目数据实例
     * @return 条目数据的文件路径，格式：[upload/yyyy/MM/dd/HH/name.postfix]
     */
    public String buildSubjectDataFilePath(FileData fileData) {
        return buildLocationDirAndReturnPath(fileData.uploadedTime())
            + File.separator + fileData.name()
            + (('.' == fileData.postfix().charAt(0))
            ? fileData.postfix() : "." + fileData.postfix());
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
    public FileDataOperateResult download(FileData fileData) {
        Assert.isNotNull(fileData);
        fileData.checkoutBeforeDownload();
        Assert.isNotNull(fileData.uploadedTime());

        String itemDataFilePath = fileData.uploadedPath();
        if (itemDataFilePath == null || "".equals(itemDataFilePath)) {
            itemDataFilePath = buildSubjectDataFilePath(fileData);
        }

        File subjectDataFile = new File(itemDataFilePath);
        if (!subjectDataFile.exists()) {
            return FileDataOperateResult
                .ofNotFound("current subject data not found, itemDataFilePath="
                    + itemDataFilePath);
        }

        try {
            byte[] datum = Files.readAllBytes(Path.of(subjectDataFile.toURI()));
            fileData.setDatum(datum);
            LOGGER.debug("download subject data success, path={}", itemDataFilePath);
        } catch (IOException e) {
            String msg = "download subject data fail for path=" + itemDataFilePath;
            LOGGER.error(msg, e);
            return FileDataOperateResult.ofDownloadFail(msg + ", exception: ", e);
        }

        return FileDataOperateResult.ofOk(fileData);
    }

    @Override
    public FileDataOperateResult delete(FileData fileData) {
        Assert.isNotNull(fileData);
        fileData.checkoutBeforeDelete();

        String subjectDataFilePath = buildSubjectDataFilePath(fileData);

        try {
            File itemDataFile = new File(subjectDataFilePath);
            itemDataFile.delete();
            LOGGER.debug("delete file success for path={}", itemDataFile);
        } catch (Exception e) {
            String msg = "delete file fail for path=" + subjectDataFilePath;
            LOGGER.error(msg, e);
            return FileDataOperateResult.ofDeleteFail(msg + ", exception: ", e);
        }

        return FileDataOperateResult.ofOk();
    }

    @Override
    public boolean exist(String uploadedPath) {
        File itemDataFile = new File(uploadedPath);
        return itemDataFile.exists();
    }
}
