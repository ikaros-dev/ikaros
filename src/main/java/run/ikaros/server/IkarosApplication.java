package run.ikaros.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.constants.FileConst;
import run.ikaros.server.exceptions.CheckFailException;
import run.ikaros.server.exceptions.IkarosException;
import run.ikaros.server.utils.FileUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.SystemVarUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.TimeZone;

/**
 * Ikaros Main Class.
 *
 * @author liguohao
 */
@SpringBootApplication
public class IkarosApplication {

    public static void main(String[] args) throws IkarosException {
        // 自定义额外的配置文件位置
        // System.setProperty("spring.config.additional-location",
        // SystemVarKit.getCurrentAppDirPath());

        String timeZone = System.getenv("IKAROS_TIME_ZONE");
        if (StringUtils.isBlank(timeZone)) {
            timeZone = "Asia/Shanghai";
        }

        // 时区设置
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));

        // 启动检查
        checkDirFileHardLink();

        // 运行应用
        SpringApplication.run(IkarosApplication.class, args);
    }

    private static void checkDirFileHardLink() throws IkarosException {
        final String currentAppDirPath = SystemVarUtils.getCurrentAppDirPath();
        File downloadsDir =
            FileUtils.createDirWhenNotExists(currentAppDirPath
                + File.separator + AppConst.DOWNLOADS);
        File mediaDir =
            FileUtils.createDirWhenNotExists(currentAppDirPath + File.separator + AppConst.MEDIA);
        File originalDir =
            FileUtils.createDirWhenNotExists(currentAppDirPath
                + File.separator + AppConst.ORIGINAL);
        File uploadDir =
            FileUtils.createDirWhenNotExists(currentAppDirPath + File.separator + AppConst.UPLOAD);

        final String testContent = "HelloIkaros";
        final String testFileName = "test.txt";
        File testDownloadFile = new File(downloadsDir.getAbsolutePath()
            + File.separator + testFileName);
        File testUploadFile = new File(uploadDir.getAbsolutePath()
            + File.separator + testFileName);
        File testMediaFile = new File(mediaDir.getAbsolutePath()
            + File.separator + testFileName);
        File testOriginalFile = new File(originalDir.getAbsolutePath()
            + File.separator + testFileName);
        try {
            if (!testDownloadFile.exists()) {
                testDownloadFile.createNewFile();
            }
            Files.writeString(testDownloadFile.toPath(), testContent);
            Files.createLink(testUploadFile.toPath(), testDownloadFile.toPath());
            Files.createLink(testMediaFile.toPath(), testDownloadFile.toPath());
            Files.createLink(testOriginalFile.toPath(), testDownloadFile.toPath());
        } catch (Exception e) {
            throw new CheckFailException("start fail, file hard link test fail", e);
        } finally {
            if (testDownloadFile.exists()) {
                testDownloadFile.delete();
            }
            if (testUploadFile.exists()) {
                testUploadFile.delete();
            }
            if (testMediaFile.exists()) {
                testMediaFile.delete();
            }
            if (testOriginalFile.exists()) {
                testOriginalFile.delete();
            }
        }

    }


}
