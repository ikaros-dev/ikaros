package run.ikaros.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.ResourceUtils;
import run.ikaros.server.rss.mikan.model.MikanRssItem;
import run.ikaros.server.utils.JsonUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SpringBootTest
class TaskServiceTest {

    @MockBean
    RssService rssService;
    @Resource
    TaskService taskService;

    /**
     * 调试需要在运行配置里加个环境变量：IKAROS_SUB_MIKAN_RSS={mikan rss http url}
     */
    @Test
    @Disabled("已经过调试")
    void pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents()
        throws IOException, URISyntaxException {
        URL mikanRssItemListJsonUrl = ResourceUtils.getURL(
            ResourceUtils.CLASSPATH_URL_PREFIX + "mikan/mikanRssItemList.json");
        byte[] bytes =
            Files.readAllBytes(Path.of(mikanRssItemListJsonUrl.toURI()));
        String mikanRssItemListJson = new String(bytes, StandardCharsets.UTF_8);

        List<MikanRssItem> mikanRssItemList =
            Arrays.stream(Objects.requireNonNull(
                JsonUtils.json2ObjArr(mikanRssItemListJson,
                    new TypeReference<MikanRssItem[]>() {
                    }))).toList();
        Mockito.doReturn(mikanRssItemList).when(rssService)
            .parseMikanMySubscribeRss(Mockito.anyString());

        taskService.pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents();
    }

    @Test
    void searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode() {
        taskService.searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();
    }
}