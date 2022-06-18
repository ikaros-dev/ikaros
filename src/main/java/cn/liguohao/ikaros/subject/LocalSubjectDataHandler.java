package cn.liguohao.ikaros.subject;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.SystemVarKit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 条目数据处理器的 本地文件系统实现。数据存放在当前程序的[upload/yyyy/MM/dd/HH/]目录下。
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public class LocalSubjectDataHandler implements SubjectDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalSubjectDataHandler.class);

    private static final String BASE_UPLOAD_DIR_NAME = "upload";

    private static final String BASE_UPLOAD_DIR_PATH
        = SystemVarKit.getCurrentAppDirPath() + File.separator + BASE_UPLOAD_DIR_NAME;


    @Override
    public SubjectDataOperateResult upload(SubjectData subjectData) {
        Assert.isNotNull(subjectData);
        subjectData.checkoutBeforeUpload();

        LocalDateTime oldUploadedTime = subjectData.uploadedTime();
        subjectData.setUploadedTime(LocalDateTime.now());

        String subjectDataFilePath = buildSubjectDataFilePath(subjectData);

        try {
            Files.write(Path.of(new File(subjectDataFilePath).toURI()), subjectData.datum());
            LOGGER.debug("upload subject data success, path={}", subjectDataFilePath);
        } catch (IOException  e) {
            subjectData.setUploadedTime(oldUploadedTime);
            String msg = "operate file fail for subjectDataFilePath=" + subjectDataFilePath;
            LOGGER.error(msg, e);
            return SubjectDataOperateResult.ofUploadFail(msg + ", exception: ", e);
        }

        return SubjectDataOperateResult.ofOk(subjectData);
    }

    /**
     * @param subjectData 条目数据实例
     * @return 条目数据的文件路径，格式：[upload/yyyy/MM/dd/HH/name.postfix]
     */
    public String buildSubjectDataFilePath(SubjectData subjectData) {
        return buildLocationDirAndReturnPath(subjectData.uploadedTime())
            + File.separator + subjectData.name()
            + (('.' == subjectData.postfix().charAt(0))
            ? subjectData.postfix() : "." + subjectData.postfix());
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
    public SubjectDataOperateResult download(SubjectData subjectData) {
        Assert.isNotNull(subjectData);
        subjectData.checkoutBeforeDownload();
        Assert.isNotNull(subjectData.uploadedTime());

        String subjectDataFilePath = buildSubjectDataFilePath(subjectData);

        File subjectDataFile = new File(subjectDataFilePath);
        if (!subjectDataFile.exists()) {
            return SubjectDataOperateResult
                .ofNotFound("current subject data not found, subjectDataFilePath="
                    + subjectDataFilePath);
        }

        try {
            byte[] datum = Files.readAllBytes(Path.of(subjectDataFile.toURI()));
            subjectData.setDatum(datum);
            LOGGER.debug("download subject data success, path={}", subjectDataFilePath);
        } catch (IOException e) {
            String msg = "download subject data fail for path=" + subjectDataFilePath;
            LOGGER.error(msg, e);
            return SubjectDataOperateResult.ofDownloadFail(msg + ", exception: ", e);
        }

        return SubjectDataOperateResult.ofOk(subjectData);
    }

    @Override
    public SubjectDataOperateResult delete(SubjectData subjectData) {
        Assert.isNotNull(subjectData);
        subjectData.checkoutBeforeDelete();

        String subjectDataFilePath = buildSubjectDataFilePath(subjectData);

        try {
            File subjectDataFile = new File(subjectDataFilePath);
            subjectDataFile.delete();
            LOGGER.debug("delete file success for subjectDataFilePath={}", subjectDataFile);
        } catch (Exception e) {
            String msg = "delete file fail for subjectDataFilePath=" + subjectDataFilePath;
            LOGGER.error(msg, e);
            return SubjectDataOperateResult.ofDeleteFail(msg + ", exception: ", e);
        }

        return SubjectDataOperateResult.ofOk();
    }
}
