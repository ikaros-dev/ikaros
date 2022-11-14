package run.ikaros.server.service;

import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;
import run.ikaros.server.core.service.RssService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.RssUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author li-guohao
 */
@Service
public class RssServiceImpl implements RssService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RssServiceImpl.class);

    @Nonnull
    @Override
    public List<MikanRssItem> parseMikanMySubscribeRss(@Nonnull String mikanMySubscribeRssUrl) {
        AssertUtils.notBlank(mikanMySubscribeRssUrl, "mikanMySubscribeRssUrl");
        List<MikanRssItem> mikanRssItemList = new ArrayList<>();
        SyndFeed syndFeed = RssUtils.parseFeed(mikanMySubscribeRssUrl);
        if (syndFeed == null || syndFeed.getEntries().size() == 0) {
            LOGGER.warn("fetch content fail, current rss subscribe address is {}",
                mikanMySubscribeRssUrl);
            return mikanRssItemList;
        }

        for (SyndEntry syndEntry : syndFeed.getEntries()) {

            String title = syndEntry.getTitle();
            List<SyndEnclosure> enclosures = syndEntry.getEnclosures();
            if (enclosures.isEmpty()) {
                LOGGER.warn("enclosures is empty, break current title={}", title);
                break;
            }
            SyndEnclosure syndEnclosure = enclosures.get(0);
            if (!"application/x-bittorrent".equalsIgnoreCase(syndEnclosure.getType())) {
                LOGGER.warn("enclosures first element type is not 'application/x-bittorrent',"
                    + " break current title={}", title);
                break;
            }

            String torrentUrl = syndEnclosure.getUrl();
            MikanRssItem mikanRssItem = new MikanRssItem();
            mikanRssItem.setTitle(title)
                .setTorrentUrl(torrentUrl)
                .setEpisodePageUrl(syndEntry.getLink());
            mikanRssItemList.add(mikanRssItem);
        }

        return mikanRssItemList;
    }
}
