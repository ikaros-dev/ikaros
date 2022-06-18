package cn.liguohao.ikaros.file;


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
class LocalItemDataHandlerTest {

    static String locationDirPath;
    static LocalItemDataHandler localItemDataHandler = new LocalItemDataHandler();
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

        ItemData itemData = new ItemData()
            .setType(ItemDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setDatum(datum)
            .checkoutBeforeUpload();

        ItemDataOperateResult result = localItemDataHandler.upload(itemData);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.subjectData());

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(result.subjectData());

        File subjectDataFile = new File(subjectDataFilePath);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));

        subjectDataFile.delete();

    }

    @Test
    void download() throws IOException {

        ItemData itemData = new ItemData()
            .setType(ItemDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(itemData);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        itemData
            .setUploadedTime(localDateTime)
            .checkoutBeforeDownload();

        ItemDataOperateResult result = localItemDataHandler.download(itemData);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.subjectData());

        Assertions.assertEquals(content,
            new String(result.subjectData().datum(), StandardCharsets.UTF_8));

        subjectDataFile.delete();

    }

    @Test
    void delete() throws IOException, InterruptedException {
        ItemData itemData = new ItemData()
            .setType(ItemDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(itemData);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        localItemDataHandler.delete(itemData.checkoutBeforeDelete());

        Thread.sleep(500);

        Assertions.assertFalse(subjectDataFile.exists());
    }
}