package run.ikaros.server.qbittorrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.exceptions.QbittorrentRequestException;
import run.ikaros.server.qbittorrent.model.QbCategory;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Component
public class QbittorrentClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(QbittorrentClient.class);
    private final RestTemplate restTemplate;
    /**
     * API前缀，例如：http://192.168.2.229:60101/api/v2/
     */
    private final String postfix;

    public interface API {
        String APP_VERSION = "/app/version";
        String APP_API_VERSION = "/app/webapiVersion";
        String TORRENTS_GET_ALL_CATEGORIES = "/torrents/categories";
        String TORRENTS_CREATE_CATEGORY = "/torrents/createCategory";
        String TORRENTS_EDIT_CATEGORY = "/torrents/editCategory";
        String TORRENTS_REMOVE_CATEGORIES = "/torrents/removeCategories";
        String TORRENTS_ADD = "/torrents/add";
    }

    public QbittorrentClient(RestTemplate restTemplate, String postfix) {
        this.restTemplate = restTemplate;
        // 如果最后一个字符是 / 则去掉
        if (postfix.charAt(postfix.length() - 1) == '/') {
            postfix = postfix.substring(0, postfix.length() - 1);
        }
        this.postfix = postfix;
    }

    public Optional<String> getApplicationVersion() {
        ResponseEntity<String> responseEntity
                = restTemplate.getForEntity(postfix + API.APP_VERSION, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("get app version fail");
        }
        return Optional.ofNullable(responseEntity.getBody());
    }

    public Optional<String> getApiVersion() {
        ResponseEntity<String> responseEntity
                = restTemplate.getForEntity(postfix + API.APP_API_VERSION, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("get app version fail");
        }
        return Optional.ofNullable(responseEntity.getBody());
    }


    public Optional<List<QbCategory>> getAllCategories() {
        List<QbCategory> qbCategoryList = new ArrayList<>();

        ResponseEntity<HashMap> responseEntity
                = restTemplate.getForEntity(postfix
                + API.TORRENTS_GET_ALL_CATEGORIES, HashMap.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("get all categories fail");
        }

        HashMap categoryMap = responseEntity.getBody();
        AssertUtils.notNull(categoryMap, "category map");
        categoryMap.forEach((key, value) -> {
            QbCategory category = JsonUtils.json2obj(String.valueOf(key), QbCategory.class);
            AssertUtils.notNull(category, "category map value");
            if (String.valueOf(key).equalsIgnoreCase(category.getName())) {
                LOGGER.warn("category map key != value's name, key={}, value={}",
                        key, value);
            }
            qbCategoryList.add(category);
        });

        return qbCategoryList.isEmpty() ? Optional.empty() : Optional.of(qbCategoryList);
    }

    public void addNewCategory(@Nonnull String category,
                               @Nonnull String savePath) {
        AssertUtils.notBlank(category, "category");
        AssertUtils.notBlank(savePath, "savePath");
        final String url = postfix + API.TORRENTS_CREATE_CATEGORY;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // body: category=CategoryName&savePath=/path/to/dir
        String body = "category=" + category + "&savePath=" + savePath;

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Object> responseEntity
                = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("add new category fail");
        }
    }

    public void editCategory(@Nonnull String category,
                             @Nonnull String savePath) {
        AssertUtils.notBlank(category, "category");
        AssertUtils.notBlank(savePath, "savePath");
        final String url = postfix + API.TORRENTS_EDIT_CATEGORY;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // body: category=CategoryName&savePath=/path/to/dir
        String body = "category=" + category + "&savePath=" + savePath;

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Object> responseEntity
                = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("edit category fail");
        }
    }

    public void removeCategories(@Nonnull List<QbCategory> categories) {
        AssertUtils.notNull(categories, "categories");
        AssertUtils.isFalse(categories.isEmpty(), "categories is empty");
        final String url = postfix + API.TORRENTS_REMOVE_CATEGORIES;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // body: categories=Category1%0ACategory2
        // categories can contain multiple categories separated by \n (%0A urlencoded)
        StringBuilder sb = new StringBuilder("categories=");
        for (int index = 0; index < categories.size(); index++) {
            sb.append(categories.get(index).getName());
            if (index < (categories.size() - 1)) {
                sb.append("%0A");
            }
        }
        String body = sb.toString();

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Object> responseEntity
                = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("remove categories fail");
        }
    }

    /**
     * @link <a href="https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)#add-new-torrent">WebUI-API-(qBittorrent-4.1)#add-new-torrent</a>
     * @param urlList url list
     * @param savepath save path
     * @param category category
     * @param newName new torrent file name
     * @param skipChecking skip hash checking
     * @param statusIsPaused add torrents in the paused state
     * @param enableSequentialDownload enable sequential download
     * @param prioritizeDownloadFirstLastPiece prioritize download first last piece
     */
    public void addTorrentFromURLs(@Nonnull List<String> urlList,
                                   @Nonnull String savepath,
                                   @Nonnull String category,
                                   @Nonnull String newName,
                                   boolean skipChecking,
                                   boolean statusIsPaused,
                                   boolean enableSequentialDownload,
                                   boolean prioritizeDownloadFirstLastPiece) {
        AssertUtils.notNull(urlList, "urlList");
        AssertUtils.isFalse(urlList.isEmpty(), "urlList is empty");
        AssertUtils.notBlank(savepath, "savepath");
        AssertUtils.notBlank(category, "category");
        AssertUtils.notBlank(newName, "newName");
        final String url = postfix + API.TORRENTS_ADD;

        // default value set

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // This method can add torrents from server local file or from URLs.
        // http://, https://, magnet: and bc://bt/ links are supported.
        Map<String, String> body = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < urlList.size(); index++) {
            sb.append(urlList.get(index));
            if (index < (urlList.size() - 1)) {
                sb.append("\\n");
            }
        }
        body.put("urls", sb.toString());
        body.put("savepath", savepath);
        body.put("category", category);
        body.put("skip_checking", skipChecking ? "true" : "false");
        body.put("paused", statusIsPaused ? "true" : "false");
        body.put("rename", newName);
        body.put("sequentialDownload", enableSequentialDownload ? "true" : "false");
        body.put("firstLastPiecePrio", prioritizeDownloadFirstLastPiece ? "true" : "false");

        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Object> responseEntity
                = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("remove categories fail");
        }
    }


}
