package run.ikaros.server.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.ikaros.server.exceptions.RssOperateException;

import java.io.IOException;
import java.net.URL;

/**
 * RSS 解析器 Rome 简单封装
 *
 * @author li-guohao
 */
public class RssUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RssUtils.class);

    /**
     * parse rss feed
     *
     * @param url rss feed str url
     * @return SyndFeed instance
     */
    public static SyndFeed parseFeed(String url) {
        SyndFeed feed = null;
        try {
            feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        } catch (FeedException | IOException e) {
            final String errMsg = "parse feed fail for url=" + url.substring(0, 30) + "...";
            LOGGER.error(errMsg, e);
            throw new RssOperateException(errMsg, e);
        }
        return feed;
    }

    /**
     * 生成RSS订阅(Feed)
     *
     * @param feed SyndFeed对象 实现类可用SyndFeedImpl
     * @return xml格式字符串
     */
    public static String generateFeed(SyndFeed feed) {
        if (StringUtils.isBlank(feed.getFeedType())) {
            feed.setFeedType("rss_2.0");
        }
        if (StringUtils.isBlank(feed.getTitle())) {
            throw new IllegalArgumentException("SyndFeed instance is not set title");
        }
        if (StringUtils.isBlank(feed.getLink())) {
            throw new IllegalArgumentException("SyndFeed instance is not set feed link");
        }
        String result = null;
        try {
            result = new SyndFeedOutput().outputString(feed);
        } catch (FeedException e) {
            final String errMsg = "generate feed fail";
            LOGGER.error(errMsg, e);
            throw new RssOperateException(errMsg, e);
        }

        return result;
    }

}
