package cn.liguohao.ikaros.model.binary;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.kit.FileKit;
import cn.liguohao.ikaros.common.kit.SystemVarKit;
import cn.liguohao.ikaros.exceptions.IkarosRuntimeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;

/**
 * @author guohao
 * @date 2022/10/21
 */
public class LocalBinaryStorge implements BinaryStorge {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalBinaryStorge.class);
    private static final String BASE_UPLOAD_DIR_NAME = "upload";

    private static final String BASE_UPLOAD_DIR_PATH
        = SystemVarKit.getCurrentAppDirPath() + File.separator + BASE_UPLOAD_DIR_NAME;

    @Override
    public Binary add(Binary binary) {
        Assert.notNull(binary, "'binary' must not be null");

        String oldUrl = binary.getOldUrl();
        String md5 = binary.getMd5();

        if (Strings.isNotBlank(oldUrl) && Strings.isNotBlank(md5)) {
            File oldFile = new File(oldUrl);
            if (oldFile.exists()) {
                try (FileInputStream fileInputStream = new FileInputStream(oldFile)) {
                    byte[] bytes = fileInputStream.readAllBytes();
                    final String oldMd5 = FileKit.checksum2Str(bytes, FileKit.Hash.MD5);
                    if (md5.equals(oldMd5)) {
                        binary.setUrl(oldUrl);
                        LOGGER.debug("repeated file, do not upload, oldUrl={}", oldUrl);
                        return binary;
                    }
                } catch (IOException ioException) {
                    LOGGER.warn("file operate fail: ", ioException);
                }
            }
        }

        binary.setPlace(BinaryPlace.LOCAL);
        binary.setUploadedTime(LocalDateTime.now());
        byte[] bytes = binary.getBytes();
        if (md5 == null) {
            binary.setMd5(FileKit.checksum2Str(bytes, FileKit.Hash.MD5));
        }

        try {
            String filePath = buildSubjectDataFilePath(binary);
            binary.setUrl(filePath);
            Files.write(Path.of(new File(filePath).toURI()), bytes);
            LOGGER.debug("upload file success, url={}", filePath);
            return binary;
        } catch (IOException e) {
            String msg = "operate file fail for upload path=" + binary.getUrl();
            LOGGER.error(msg + ": ", e);
            throw new IkarosRuntimeException(msg);
        }
    }

    /**
     * @return 文件路径，格式：[upload/yyyy/MM/dd/HH/随机生成的UUID.postfix]
     */
    private String buildSubjectDataFilePath(Binary binary) {
        Assert.notNull(binary, "'binary' must not be null");
        String name = binary.getName();
        Assert.notBlank(name, "'name' must not be blank");

        String postfix = name.substring(name.lastIndexOf("."));

        return buildLocationDirAndReturnPath(binary.getUploadedTime())
            + File.separator + UUID.randomUUID().toString().replace("-", "")
            + (('.' == postfix.charAt(0))
            ? postfix : "." + postfix);
    }

    /**
     * @param uploadedTime 条目数据上传的时间
     * @return 基础的上传目录路径，格式：[upload/yyyy/MM/dd/HH]
     */
    private String buildLocationDirAndReturnPath(LocalDateTime uploadedTime) {
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

    @Retryable
    @Override
    public void delete(Binary binary) {
        Assert.notNull(binary, "'binary' must not be null");
        String url = binary.getUrl();
        Assert.notBlank(url, "'url' must not be empty");
        File file = new File(url);
        if (file.exists()) {
            if (!file.delete()) {
                throw new IkarosRuntimeException("file delete fail");
            } else {
                LOGGER.debug("delete file success, url={}", url);
            }
        }
    }

    @Override
    public boolean exists(Binary binary) {
        Assert.notNull(binary, "'binary' must not be null");
        String url = binary.getUrl();
        Assert.notBlank(url, "'url' must not be empty");
        return new File(url).exists();
    }

    @Override
    public BinaryPlace getPlace(Binary binary) {
        Assert.notNull(binary, "'binary' must not be null");
        return binary.getPlace();
    }
}
