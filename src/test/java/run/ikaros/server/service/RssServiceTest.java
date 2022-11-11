package run.ikaros.server.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.server.rss.mikan.model.MikanRssItem;
import run.ikaros.server.service.impl.RssServiceImpl;

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