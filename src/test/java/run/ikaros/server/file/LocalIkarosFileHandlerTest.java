package run.ikaros.server.file;


import run.ikaros.server.file.IkarosFile;
import run.ikaros.server.file.IkarosFileOperateResult;
import run.ikaros.server.file.LocalIkarosFileHandler;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
@Disabled
class LocalIkarosFileHandlerTest {

    static String locationDirPath;
    static LocalIkarosFileHandler localItemDataHandler = new LocalIkarosFileHandler();
    static String content = "Hello World.";
    static LocalDateTime localDateTime = LocalDateTime.now();

    @BeforeAll
    static void setUp() {
        locationDirPath = localItemDataHandler
            .buildLocationDirAndReturnPath(localDateTime);

    }

    @AfterAll
    static void setDown() {
        File locationDir = new File(locationDirPath);
        locationDir.deleteOnExit();
    }

    @Test
    void upload() throws IOException {

        byte[] datum = content.getBytes(StandardCharsets.UTF_8);

        IkarosFile ikarosFile = new IkarosFile()
            .setType(IkarosFile.Type.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setBytes(datum)
            .checkoutBeforeUpload();

        IkarosFileOperateResult result = localItemDataHandler.upload(ikarosFile);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getIkarosFile());

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(result.getIkarosFile());

        File subjectDataFile = new File(subjectDataFilePath);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));

        subjectDataFile.delete();

    }

    @Test
    void download() throws IOException {

        IkarosFile ikarosFile = new IkarosFile()
            .setType(IkarosFile.Type.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(ikarosFile);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        ikarosFile
            .setUploadedTime(localDateTime)
            .checkoutBeforeDownload();

        IkarosFileOperateResult result = localItemDataHandler.download(ikarosFile);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getIkarosFile());

        Assertions.assertEquals(content,
            new String(result.getIkarosFile().getBytes(), StandardCharsets.UTF_8));

        subjectDataFile.delete();

    }

    @Test
    void delete() throws IOException, InterruptedException {
        IkarosFile ikarosFile = new IkarosFile()
            .setType(IkarosFile.Type.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(ikarosFile);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        localItemDataHandler.delete(ikarosFile.checkoutBeforeDelete());

        Thread.sleep(500);

        Assertions.assertFalse(subjectDataFile.exists());
    }
}