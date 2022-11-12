package run.ikaros.server.service;

import org.springframework.retry.annotation.Retryable;
import run.ikaros.server.rss.mikan.model.MikanRssItem;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author li-guohao
 */
public interface RssService {

    @Nonnull
    @Retryable
    List<MikanRssItem> parseMikanMySubscribeRss(@Nonnull String mikanMySubscribeRssUrl);

}
