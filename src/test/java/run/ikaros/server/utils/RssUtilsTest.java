package run.ikaros.server.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author li-guohao
 */
class RssUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RssUtilsTest.class);

    @Test
    void parseFeed() {
        String url = "http://www.comicat.org/rss-%E6%A1%9C%E9%83%BD%E5%AD%97%E5%B9%95%E7%BB%84.xml";
        SyndFeed syndFeed = RssUtils.parseFeed(url);
        LOGGER.info("Title: " + syndFeed.getTitle());
        LOGGER.info("Author: " + syndFeed.getAuthor());
        LOGGER.info("FeedType: " + syndFeed.getFeedType());
        LOGGER.info("Description: " + syndFeed.getDescription());
        LOGGER.info("Copyright: " + syndFeed.getCopyright());
        LOGGER.info("Link: " + syndFeed.getLink());
        LOGGER.info("Language: " + syndFeed.getLanguage());
        LOGGER.info("Encoding: " + syndFeed.getEncoding());
        LOGGER.info("Generator: " + syndFeed.getGenerator());
        LOGGER.info("Uri: " + syndFeed.getUri());
        LOGGER.info("Authors: " + syndFeed.getAuthors());
        LOGGER.info("Docs: " + syndFeed.getDocs());
        LOGGER.info("Entries Size: " + syndFeed.getEntries().size());
        assertNotNull(syndFeed);
        assertNotNull(syndFeed.getEntries());
        assertTrue(syndFeed.getEntries().size() > 0);
    }

    @Test
    void generateFeed() {
        String title = "hello rss";
        String description = "rss feed desc";
        String feedLink = "https://liguohao.cn/rss";
        SyndFeedImpl syndFeed = new SyndFeedImpl();
        syndFeed.setTitle(title);
        syndFeed.setDescription(description);
        syndFeed.setLink(feedLink);
        String feed = RssUtils.generateFeed(syndFeed);
        assertNotNull(feed);
    }
}