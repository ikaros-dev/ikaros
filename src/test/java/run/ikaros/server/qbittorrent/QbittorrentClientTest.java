package run.ikaros.server.qbittorrent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.bt.qbittorrent.QbittorrentClient;
import run.ikaros.server.common.UnitTestConst;
import run.ikaros.server.exceptions.QbittorrentRequestException;
import run.ikaros.server.bt.qbittorrent.enums.QbTorrentInfoFilter;
import run.ikaros.server.bt.qbittorrent.model.QbCategory;
import run.ikaros.server.bt.qbittorrent.model.QbTorrentInfo;
import run.ikaros.server.utils.FileUtils;

/**
 * @author li-guohao
 */
//@Disabled("only local test can pass")
class QbittorrentClientTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QbittorrentClientTest.class);

    static String prefix = "http://192.168.2.229:60101/api/v2";
    static final String hash = "42b6ca3fa47fa5435ad69ce67fd7611237bdec5a";
    static RestTemplate restTemplate = new RestTemplate();
    static QbittorrentClient qbittorrentClient = new QbittorrentClient(restTemplate, prefix);
    static final String category = "unittest";
    static final String savePath = "/downloads/unittest";

    @BeforeEach
    void setUp() {
        qbittorrentClient.removeCategories(List.of(category));
    }

    @Test
    void getApplicationVersion() {
        String appVersion = qbittorrentClient.getApplicationVersion();
        Assertions.assertNotNull(appVersion);
        LOGGER.info("app version: {}", appVersion);
    }

    @Test
    void getApiVersion() {
        String apiVersion = qbittorrentClient.getApiVersion();
        Assertions.assertNotNull(apiVersion);
        LOGGER.info("api version: {}", apiVersion);
    }

    @Test
    void getAllCategories() {
        qbittorrentClient.getAllCategories();
    }

    private void assertCategory(boolean exceptExist) {
        Assertions.assertNotNull(category);
        List<QbCategory> categories = qbittorrentClient.getAllCategories();
        boolean exist = false;
        if (!categories.isEmpty()) {
            exist = categories.stream().anyMatch(
                qbCategory -> (category.equalsIgnoreCase(qbCategory.getName())
                    && savePath.equalsIgnoreCase(qbCategory.getSavePath())));
        }

        if (exceptExist) {
            Assertions.assertTrue(exist);
        } else {
            Assertions.assertFalse(exist);
        }
    }


    @Test
    void addNewCategory() {
        assertCategory(false);
        qbittorrentClient.addNewCategory(category, savePath);
        assertCategory(true);

        try {
            qbittorrentClient.addNewCategory(category, savePath);
            Assertions.fail(UnitTestConst.PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (HttpClientErrorException httpClientErrorException) {
            assertSame(httpClientErrorException.getStatusCode(), HttpStatus.CONFLICT);
        }

        qbittorrentClient.removeCategories(List.of(category));
    }


    @Test
    void editCategory() {
        qbittorrentClient.addNewCategory(category, savePath);
        assertCategory(true);

        final String newSavePath = "/downloads/unittest/new";
        qbittorrentClient.editCategory(category, newSavePath);

        Optional<QbCategory> categoryOptional = qbittorrentClient.getAllCategories().stream()
            .filter(qbCategory -> category.equalsIgnoreCase(qbCategory.getName()))
            .findFirst();
        Assertions.assertTrue(categoryOptional.isPresent());
        String updatedSavePath = categoryOptional.get().getSavePath();
        Assertions.assertNotNull(updatedSavePath);
        Assertions.assertNotEquals(savePath, updatedSavePath);
        Assertions.assertEquals(newSavePath, updatedSavePath);

        qbittorrentClient.removeCategories(List.of(category));
    }

    @Test
    void removeCategories() {
        assertCategory(false);
        qbittorrentClient.addNewCategory(category, savePath);
        assertCategory(true);
        qbittorrentClient.removeCategories(List.of(category));
        assertCategory(false);
    }

    @Test
    @Disabled
    void addTorrentFromURLs() throws IOException, URISyntaxException {
        final String fileName = "Shinmai Renkinjutsushi no Tenpo Keiei";
        URL url = ResourceUtils.getURL(ResourceUtils.CLASSPATH_URL_PREFIX + "qbittorrent/" + fileName);
        List<String> urlList = Files.readAllLines(Path.of(url.toURI()), StandardCharsets.UTF_8);
        qbittorrentClient.addNewCategory(category, savePath);
        for (String src : urlList) {
            qbittorrentClient.addTorrentFromURLs(src, savePath + "/" + fileName,
                category, null, true, false, true, true);
        }

    }

    @Test
    @Disabled
    void getTorrentList() {
        List<QbTorrentInfo> torrentList =
            qbittorrentClient.getTorrentList(QbTorrentInfoFilter.ALL, category, null, 100, 0, null);
        Assertions.assertFalse(torrentList.isEmpty());
        torrentList.forEach(
            qbTorrentInfo -> LOGGER.info("[{}] state={}", qbTorrentInfo.getName(),
                qbTorrentInfo.getState()));
    }

    @Test
    @Disabled
    void getTorrentListAppoint() {
        List<QbTorrentInfo> torrentList =
            qbittorrentClient.getTorrentList(QbTorrentInfoFilter.ALL, null, null, null, null, hash);
        Assertions.assertFalse(torrentList.isEmpty());
        torrentList.forEach(
            qbTorrentInfo -> LOGGER.info("[{}] state={}", qbTorrentInfo.getName(),
                qbTorrentInfo.getState()));
    }

    @Test
    @Disabled
    void renameFile() {
        List<QbTorrentInfo> torrentList =
            qbittorrentClient.getTorrentList(null, null, null, null, null,
                hash);
        QbTorrentInfo qbTorrentInfo = torrentList.get(0);

        String contentPath = qbTorrentInfo.getContentPath();

        String oldFileName = FileUtils.parseFileName(contentPath);
        String[] splitArr = oldFileName.split("\\.");
        String newFileName = splitArr[0] + "-NEW" + "." + splitArr[1];

        qbittorrentClient.renameFile(hash, oldFileName, newFileName);


        List<QbTorrentInfo> updatedTorrentList =
            qbittorrentClient.getTorrentList(null, null, null, null, null, hash);
        QbTorrentInfo updatedQbTorrentInfo = updatedTorrentList.get(0);
        Assertions.assertEquals(newFileName, FileUtils.parseFileName(updatedQbTorrentInfo.getContentPath()));
    }

    @Test
    @Disabled
    void resume() throws InterruptedException {
        QbTorrentInfo torrent = qbittorrentClient.getTorrent(hash);
        String state = torrent.getState();
        qbittorrentClient.resume(hash);
        Thread.sleep(250);
        torrent = qbittorrentClient.getTorrent(hash);
        state = torrent.getState();
    }

    @Test
    @Disabled
    void pause() throws InterruptedException {
        QbTorrentInfo torrent = qbittorrentClient.getTorrent(hash);
        String state = torrent.getState();
        qbittorrentClient.pause(hash);
        Thread.sleep(1000);
        torrent = qbittorrentClient.getTorrent(hash);
        state = torrent.getState();
    }

    @Test
    @Disabled
    void recheck() throws InterruptedException {
        QbTorrentInfo torrent = qbittorrentClient.getTorrent(hash);
        String state = torrent.getState();
        qbittorrentClient.recheck(hash);
        Thread.sleep(1000);
        torrent = qbittorrentClient.getTorrent(hash);
        state = torrent.getState();
    }

    @Test
    @Disabled
    void delete() throws InterruptedException {
        QbTorrentInfo torrent = qbittorrentClient.getTorrent(hash);
        String state = torrent.getState();
        qbittorrentClient.delete(hash, true);
        Thread.sleep(1000);
        try {
            torrent = qbittorrentClient.getTorrent(hash);
            Assertions.fail(UnitTestConst.PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (QbittorrentRequestException qbittorrentRequestException) {
            Assertions.assertNotNull(qbittorrentRequestException);
        }
    }
}