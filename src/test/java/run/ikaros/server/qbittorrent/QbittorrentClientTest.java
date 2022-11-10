package run.ikaros.server.qbittorrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.common.UnitTestConst;
import run.ikaros.server.qbittorrent.model.QbCategory;

/**
 * @author li-guohao
 */
class QbittorrentClientTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QbittorrentClientTest.class);

    static String prefix = "http://192.168.2.229:60101/api/v2";
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

    private void assertCategory(boolean exceptExist){
        Assertions.assertNotNull(category);
        List<QbCategory> categories = qbittorrentClient.getAllCategories();
        boolean exist = false;
        if(!categories.isEmpty()) {
            exist = categories.stream().anyMatch(
                qbCategory -> (category.equalsIgnoreCase(qbCategory.getName())
                    && savePath.equalsIgnoreCase(qbCategory.getSavePath())));
        }

        if(exceptExist) {
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
    void addTorrentFromURLs() {
        // todo impl
    }
}