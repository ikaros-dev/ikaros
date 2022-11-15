package run.ikaros.server.core.service;

import org.springframework.retry.annotation.Retryable;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * @author li-guohao
 */
public interface RssService {

    @Nonnull
    List<MikanRssItem> parseMikanMySubscribeRss(@Nonnull String mikanMySubscribeRssUrl);

    @Retryable
    String downloadRssXmlFile(@Nonnull String url);

}
