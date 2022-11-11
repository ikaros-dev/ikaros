package run.ikaros.server.service;

import java.util.List;
import javax.annotation.Nonnull;
import run.ikaros.server.rss.mikan.model.MikanRssItem;

/**
 * @author li-guohao
 */
public interface RssService {

    @Nonnull
    List<MikanRssItem> parseMikanMySubscribeRss(@Nonnull String mikanMySubscribeRssUrl);

}
