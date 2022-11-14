package run.ikaros.server.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.server.core.service.RssService;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;

import java.util.List;

/**
 * @author li-guohao
 */
class RssServiceTest {

    RssService rssService = new RssServiceImpl();

    /**
     * need add run env config: IKAROS_TEST_RSS_MIKAN_MY_SUB_URL=http://xxxxx
     */
    @Test
    void parseMikanMySubscribeRss() {
        final String url = System.getenv("IKAROS_TEST_RSS_MIKAN_MY_SUB_URL");
        List<MikanRssItem> mikanRssItemList = rssService.parseMikanMySubscribeRss(url);
        Assertions.assertFalse(mikanRssItemList.isEmpty());
    }
}