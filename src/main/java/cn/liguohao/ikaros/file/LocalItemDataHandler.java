package cn.liguohao.ikaros.file;

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
public class LocalItemDataHandler implements ItemDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalItemDataHandler.class);

    private static final String BASE_UPLOAD_DIR_NAME = "upload";

    private static final String BASE_UPLOAD_DIR_PATH
        = SystemVarKit.getCurrentAppDirPath() + File.separator + BASE_UPLOAD_DIR_NAME;


    @Override
    public ItemDataOperateResult upload(ItemData itemData) {
        Assert.isNotNull(itemData);
        itemData.checkoutBeforeUpload();

        LocalDateTime oldUploadedTime = itemData.uploadedTime();
        itemData.setUploadedTime(LocalDateTime.now());

        String subjectDataFilePath = buildSubjectDataFilePath(itemData);

        try {
            Files.write(Path.of(new File(subjectDataFilePath).toURI()), itemData.datum());
            itemData.setUploadedPath(subjectDataFilePath);
            LOGGER.debug("upload subject data success, path={}", subjectDataFilePath);
        } catch (IOException e) {
            itemData.setUploadedTime(oldUploadedTime);
            String msg = "operate file fail for subjectDataFilePath=" + subjectDataFilePath;
            LOGGER.error(msg, e);
            return ItemDataOperateResult.ofUploadFail(msg + ", exception: ", e);
        }

        return ItemDataOperateResult.ofOk(itemData);
    }

    /**
     * @param itemData 条目数据实例
     * @return 条目数据的文件路径，格式：[upload/yyyy/MM/dd/HH/name.postfix]
     */
    public String buildSubjectDataFilePath(ItemData itemData) {
        return buildLocationDirAndReturnPath(itemData.uploadedTime())
            + File.separator + itemData.name()
            + (('.' == itemData.postfix().charAt(0))
            ? itemData.postfix() : "." + itemData.postfix());
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
    public ItemDataOperateResult download(ItemData itemData) {
        Assert.isNotNull(itemData);
        itemData.checkoutBeforeDownload();
        Assert.isNotNull(itemData.uploadedTime());

        String itemDataFilePath = itemData.uploadedPath();
        if (itemDataFilePath == null || "".equals(itemDataFilePath)) {
            itemDataFilePath = buildSubjectDataFilePath(itemData);
        }

        File subjectDataFile = new File(itemDataFilePath);
        if (!subjectDataFile.exists()) {
            return ItemDataOperateResult
                .ofNotFound("current subject data not found, itemDataFilePath="
                    + itemDataFilePath);
        }

        try {
            byte[] datum = Files.readAllBytes(Path.of(subjectDataFile.toURI()));
            itemData.setDatum(datum);
            LOGGER.debug("download subject data success, path={}", itemDataFilePath);
        } catch (IOException e) {
            String msg = "download subject data fail for path=" + itemDataFilePath;
            LOGGER.error(msg, e);
            return ItemDataOperateResult.ofDownloadFail(msg + ", exception: ", e);
        }

        return ItemDataOperateResult.ofOk(itemData);
    }

    @Override
    public ItemDataOperateResult delete(ItemData itemData) {
        Assert.isNotNull(itemData);
        itemData.checkoutBeforeDelete();

        String subjectDataFilePath = buildSubjectDataFilePath(itemData);

        try {
            File itemDataFile = new File(subjectDataFilePath);
            itemDataFile.delete();
            LOGGER.debug("delete file success for path={}", itemDataFile);
        } catch (Exception e) {
            String msg = "delete file fail for path=" + subjectDataFilePath;
            LOGGER.error(msg, e);
            return ItemDataOperateResult.ofDeleteFail(msg + ", exception: ", e);
        }

        return ItemDataOperateResult.ofOk();
    }

    @Override
    public boolean exist(String uploadedPath) {
        File itemDataFile = new File(uploadedPath);
        return itemDataFile.exists();
    }
}
