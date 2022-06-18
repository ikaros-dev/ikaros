package cn.liguohao.ikaros.subject;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
class LocalSubjectDataHandlerTest {

    static String locationDirPath;
    static LocalSubjectDataHandler localSubjectDataHandler = new LocalSubjectDataHandler();
    static String content = "Hello World.";
    static LocalDateTime localDateTime = LocalDateTime.now();

    @BeforeAll
    static void setUp() {
        locationDirPath = localSubjectDataHandler
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

        SubjectData subjectData = new SubjectData()
            .setType(SubjectDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setDatum(datum)
            .checkoutBeforeUpload();

        SubjectDataOperateResult result = localSubjectDataHandler.upload(subjectData);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.subjectData());

        String subjectDataFilePath
            = localSubjectDataHandler.buildSubjectDataFilePath(result.subjectData());

        File subjectDataFile = new File(subjectDataFilePath);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));

        subjectDataFile.delete();

    }

    @Test
    void download() throws IOException {

        SubjectData subjectData = new SubjectData()
            .setType(SubjectDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localSubjectDataHandler.buildSubjectDataFilePath(subjectData);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        subjectData
            .setUploadedTime(localDateTime)
            .checkoutBeforeDownload();

        SubjectDataOperateResult result = localSubjectDataHandler.download(subjectData);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.subjectData());

        Assertions.assertEquals(content,
            new String(result.subjectData().datum(), StandardCharsets.UTF_8));

        subjectDataFile.delete();

    }

    @Test
    void delete() throws IOException, InterruptedException {
        SubjectData subjectData = new SubjectData()
            .setType(SubjectDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localSubjectDataHandler.buildSubjectDataFilePath(subjectData);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        localSubjectDataHandler.delete(subjectData.checkoutBeforeDelete());

        Thread.sleep(500);

        Assertions.assertFalse(subjectDataFile.exists());
    }
}