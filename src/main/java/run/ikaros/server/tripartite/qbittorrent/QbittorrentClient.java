package run.ikaros.server.tripartite.qbittorrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import run.ikaros.server.tripartite.qbittorrent.enums.QbTorrentInfoFilter;
import run.ikaros.server.tripartite.qbittorrent.model.QbCategory;
import run.ikaros.server.tripartite.qbittorrent.model.QbTorrentInfo;
import run.ikaros.server.exceptions.QbittorrentRequestException;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @link <a href="https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)">WebUI-API-(qBittorrent-4.1)</a>
 */
@Component
@Retryable
public class QbittorrentClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(QbittorrentClient.class);
    private String category = "ikaros";
    private String categorySavePath = "/downloads/ikaros/";
    private final RestTemplate restTemplate = new RestTemplate();
    /**
     * API前缀，例如：http://192.168.2.229:60101/api/v2/
     */
    private final String prefix;

    public interface API {
        String APP_VERSION = "/app/version";
        String APP_API_VERSION = "/app/webapiVersion";
        String TORRENTS_GET_ALL_CATEGORIES = "/torrents/categories";
        String TORRENTS_CREATE_CATEGORY = "/torrents/createCategory";
        String TORRENTS_EDIT_CATEGORY = "/torrents/editCategory";
        String TORRENTS_REMOVE_CATEGORIES = "/torrents/removeCategories";
        String TORRENTS_ADD = "/torrents/add";
        String TORRENTS_INFO = "/torrents/info";
        String TORRENTS_RENAME_FILE = "/torrents/renameFile";
        String TORRENTS_RESUME = "/torrents/resume";
        String TORRENTS_PAUSE = "/torrents/pause";
        String TORRENTS_DELETE = "/torrents/delete";
        String TORRENTS_RECHECK = "/torrents/recheck";
    }

    public QbittorrentClient(@Value("${ikaros.qbittorrent-base-url}") String prefix) {
        // 如果最后一个字符是 / 则去掉
        if (prefix.charAt(prefix.length() - 1) == '/') {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        this.prefix = prefix;
    }

    public String getCategory() {
        return category;
    }

    public String getPrefix() {
        return prefix;
    }

    @PostConstruct
    public void initQbittorrentCategory() {
        try {
            LOGGER.debug("prefix={}", prefix);
            boolean exist = getAllCategories().stream()
                .anyMatch(qbCategory -> qbCategory.getName().equalsIgnoreCase(category));
            if (!exist) {
                addNewCategory(category, categorySavePath);
                LOGGER.debug("add new qbittorrent category: {}, savePath: {}",
                    category, categorySavePath);
            }
        } catch (Exception exception) {
            LOGGER.warn("operate fail for add qbittorrent category: {}", category, exception);
        }
    }

    public String getApplicationVersion() {
        ResponseEntity<String> responseEntity
            = restTemplate.getForEntity(prefix + API.APP_VERSION, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("get app version fail");
        }
        return responseEntity.getBody();
    }

    public String getApiVersion() {
        ResponseEntity<String> responseEntity
            = restTemplate.getForEntity(prefix + API.APP_API_VERSION, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new QbittorrentRequestException("get app version fail");
        }
        return responseEntity.getBody();
    }


    @SuppressWarnings("unchecked")
    public List<QbCategory> getAllCategories() {
        List<QbCategory> qbCategoryList = new ArrayList<>();

        HashMap<String, Object> categoryMap
            = restTemplate.getForObject(prefix
            + API.TORRENTS_GET_ALL_CATEGORIES, HashMap.class);

        AssertUtils.notNull(categoryMap, "category map");
        categoryMap.forEach((key, value) -> {
            Map<String, String> valueMap = (Map<String, String>) value;
            QbCategory category = new QbCategory();
            category.setName(valueMap.get("name"));
            category.setSavePath(valueMap.get("savePath"));
            AssertUtils.notNull(category, "category map value");
            if (!String.valueOf(key).equalsIgnoreCase(category.getName())) {
                LOGGER.warn("category map key != value's name, key={}, value={}",
                    key, value);
            }
            qbCategoryList.add(category);
        });

        return qbCategoryList;
    }

    public void addNewCategory(@Nonnull String category,
                               @Nonnull String savePath) {
        AssertUtils.notBlank(category, "category");
        AssertUtils.notBlank(savePath, "savePath");
        final String url = prefix + API.TORRENTS_CREATE_CATEGORY;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // body: category=CategoryName&savePath=/path/to/dir
        String body = "category=" + category + "&savePath=" + savePath;

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);
    }

    public void editCategory(@Nonnull String category,
                             @Nonnull String savePath) {
        AssertUtils.notBlank(category, "category");
        AssertUtils.notBlank(savePath, "savePath");
        final String url = prefix + API.TORRENTS_EDIT_CATEGORY;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // body: category=CategoryName&savePath=/path/to/dir
        String body = "category=" + category + "&savePath=" + savePath;

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);
    }

    public void removeCategories(@Nonnull List<String> categories) {
        AssertUtils.notNull(categories, "categories");
        AssertUtils.isFalse(categories.isEmpty(), "categories is empty");
        final String url = prefix + API.TORRENTS_REMOVE_CATEGORIES;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // body: categories=Category1%0ACategory2
        // categories can contain multiple categories separated by \n (%0A urlencoded)
        StringBuilder sb = new StringBuilder("categories=");
        for (int index = 0; index < categories.size(); index++) {
            sb.append(categories.get(index));
            if (index < (categories.size() - 1)) {
                sb.append("%0A");
            }
        }
        String body = sb.toString();

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);
    }

    /**
     * @param src                              src
     * @param savepath                         save path
     * @param category                         category
     * @param newName                          new torrent file name
     * @param skipChecking                     skip hash checking
     * @param statusIsPaused                   add torrents in the paused state
     * @param enableSequentialDownload         enable sequential download
     * @param prioritizeDownloadFirstLastPiece prioritize download first last piece,
     *                                         开启的话，经常会出现丢失文件的错误，不建议开启
     * @link <a href="https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)#add-new-torrent">WebUI-API-(qBittorrent-4.1)#add-new-torrent</a>
     */
    public synchronized void addTorrentFromURLs(@Nonnull String src,
                                                String savepath,
                                                @Nonnull String category,
                                                String newName,
                                                boolean skipChecking,
                                                boolean statusIsPaused,
                                                boolean enableSequentialDownload,
                                                boolean prioritizeDownloadFirstLastPiece) {
        AssertUtils.notNull(src, "src");
        AssertUtils.notBlank(category, "category");
        final String url = prefix + API.TORRENTS_ADD;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // This method can add torrents from server local file or from URLs.
        // http://, https://, magnet: and bc://bt/ links are supported.
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.put("urls", List.of(src));
        if (StringUtils.isNotBlank(savepath)) {
            body.put("savepath", List.of(savepath));
        }
        body.put("category", List.of(category));
        body.put("skip_checking", List.of(skipChecking ? "true" : "false"));
        body.put("paused", List.of(statusIsPaused ? "true" : "false"));
        if (StringUtils.isNotBlank(newName)) {
            body.put("rename", List.of(newName));
        }
        body.put("sequentialDownload", List.of(enableSequentialDownload ? "true" : "false"));
        body.put("firstLastPiecePrio",
            List.of(prioritizeDownloadFirstLastPiece ? "true" : "false"));

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
    }

    public void addTorrentFromUrl(@Nonnull String url) {
        AssertUtils.notBlank(url, "url");
        addTorrentFromURLs(url, categorySavePath, category, null, true,
            false, false, false);
    }

    /**
     * @param filter   Filter torrent list by state. Allowed state filters: all, downloading,
     *                 seeding,completed, paused, active, inactive, resumed, stalled,
     *                 stalled_uploading,stalled_downloading, errored
     * @param category Get torrents with the given category (empty string means "without category";
     *                 no "category" parameter means "any category" <- broken until
     *                 #11748 is resolved). Remember to URL-encode the category name. For example,
     *                 My category becomes My%20category
     * @param limit    Limit the number of torrents returned
     * @param offset   Set offset (if less than 0, offset from end)
     * @param hashes   Filter by hashes. Can contain multiple hashes separated by |
     * @return qbittorrent torrent info list
     * @link <a href="https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)#get-torrent-list">WebUI-API-(qBittorrent-4.1)#get-torrent-list</a>
     * @see QbTorrentInfoFilter
     */
    public List<QbTorrentInfo> getTorrentList(QbTorrentInfoFilter filter,
                                              String category, String tags,
                                              Integer limit, Integer offset,
                                              String hashes) {
        final String url = prefix + API.TORRENTS_INFO;

        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(url);
        if (filter != null) {
            urlBuilder.queryParam("filter", filter.getValue());
        }
        if (StringUtils.isNotBlank(category)) {
            urlBuilder.queryParam("category", category);
        }
        if (StringUtils.isNotBlank(tags)) {
            urlBuilder.queryParam("tags", tags);
        }
        if (limit != null && limit > 0) {
            urlBuilder.queryParam("limit", limit);
        }
        if (offset != null && offset > 0) {
            urlBuilder.queryParam("offset", offset);
        }
        if (StringUtils.isNotBlank(hashes)) {
            urlBuilder.queryParam("hashes", hashes);
        }

        ArrayList originalList =
            restTemplate.getForObject(urlBuilder.toUriString(), ArrayList.class);
        AssertUtils.notNull(originalList, "originalList");

        List<QbTorrentInfo> qbTorrentInfoList = new ArrayList<>(originalList.size());
        for (Object o : originalList) {
            QbTorrentInfo qbTorrentInfo
                = JsonUtils.json2obj(JsonUtils.obj2Json(o), QbTorrentInfo.class);
            qbTorrentInfoList.add(qbTorrentInfo);
        }

        return qbTorrentInfoList;
    }

    public QbTorrentInfo getTorrent(String hash) {
        AssertUtils.notBlank(hash, "hash");
        List<QbTorrentInfo> torrentList = getTorrentList(null, null, null, null, null, hash);
        if (torrentList.isEmpty()) {
            throw new QbittorrentRequestException("torrent not found for hash=" + hash);
        }
        return torrentList.get(0);
    }

    /**
     * rename torrent file
     *
     * @param hash        The hash of the torrent
     * @param oldFileName The old file name(with postfix) of the torrent's file
     * @param newFileName The new file name(with postfix) to use for the file
     * @link <a href="https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)#rename-file">WebUI-API-(qBittorrent-4.1)#rename-file</a>
     */
    public void renameFile(@Nonnull String hash, @Nonnull String oldFileName,
                           @Nonnull String newFileName) {
        AssertUtils.notBlank(hash, "hash");
        AssertUtils.notBlank(oldFileName, "oldFileName");
        AssertUtils.notBlank(newFileName, "newFileName");
        final String url = prefix + API.TORRENTS_RENAME_FILE;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        String body = "hash=" + hash + "&oldPath="
            + URLEncoder.encode(oldFileName, StandardCharsets.UTF_8)
            + "&newPath=" + URLEncoder.encode(newFileName, StandardCharsets.UTF_8);

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
    }

    public void resume(@Nonnull String hashes) {
        AssertUtils.notBlank(hashes, "hashes");
        final String url = prefix + API.TORRENTS_RESUME;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        String body = "hashes=" + hashes;

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
    }

    public void pause(@Nonnull String hashes) {
        AssertUtils.notBlank(hashes, "hashes");
        final String url = prefix + API.TORRENTS_PAUSE;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        String body = "hashes=" + hashes;

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
    }

    public void recheck(@Nonnull String hashes) {
        AssertUtils.notBlank(hashes, "hashes");
        final String url = prefix + API.TORRENTS_RECHECK;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        String body = "hashes=" + hashes;

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
    }

    public void delete(@Nonnull String hashes, Boolean deleteFiles) {
        AssertUtils.notBlank(hashes, "hashes");
        final String url = prefix + API.TORRENTS_DELETE;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        String body = "hashes=" + hashes
            + "&deleteFiles=" + (deleteFiles ? "true" : "false");

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
    }
}
