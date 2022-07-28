package cn.liguohao.ikaros.file;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import cn.liguohao.ikaros.persistence.incompact.file.FileData;
import cn.liguohao.ikaros.persistence.incompact.file.FileDataOperateResult;
import cn.liguohao.ikaros.persistence.incompact.file.FileDataType;
import cn.liguohao.ikaros.persistence.incompact.file.LocalFileDataHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
class LocalFileDataHandlerTest {

    static String locationDirPath;
    static LocalFileDataHandler localItemDataHandler = new LocalFileDataHandler();
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

        FileData fileData = new FileData()
            .setType(FileDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setDatum(datum)
            .checkoutBeforeUpload();

        FileDataOperateResult result = localItemDataHandler.upload(fileData);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.itemData());

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(result.itemData());

        File subjectDataFile = new File(subjectDataFilePath);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));

        subjectDataFile.delete();

    }

    @Test
    void download() throws IOException {

        FileData fileData = new FileData()
            .setType(FileDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(fileData);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        fileData
            .setUploadedTime(localDateTime)
            .checkoutBeforeDownload();

        FileDataOperateResult result = localItemDataHandler.download(fileData);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.itemData());

        Assertions.assertEquals(content,
            new String(result.itemData().datum(), StandardCharsets.UTF_8));

        subjectDataFile.delete();

    }

    @Test
    void delete() throws IOException, InterruptedException {
        FileData fileData = new FileData()
            .setType(FileDataType.DOCUMENT)
            .setName("test")
            .setPostfix(".txt")
            .setUploadedTime(localDateTime);

        String subjectDataFilePath
            = localItemDataHandler.buildSubjectDataFilePath(fileData);

        File subjectDataFile = new File(subjectDataFilePath);

        Files.writeString(Path.of(subjectDataFile.toURI()), content);

        Assertions.assertTrue(subjectDataFile.exists());
        Assertions.assertTrue(subjectDataFile.isFile());

        Assertions.assertEquals(content,
            Files.readString(Path.of(subjectDataFile.toURI())));


        localItemDataHandler.delete(fileData.checkoutBeforeDelete());

        Thread.sleep(500);

        Assertions.assertFalse(subjectDataFile.exists());
    }
}