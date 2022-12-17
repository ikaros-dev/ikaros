package run.ikaros.server.core.service;

import org.springframework.lang.Nullable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.service.RssServiceImpl;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.Proxy;
import java.util.List;

/**
 * @author li-guohao
 */
public interface RssService {

    void setProxy(@Nullable Proxy proxy);

    @Nonnull
    List<MikanRssItem> parseMikanMySubscribeRss(@Nonnull String mikanMySubscribeRssUrl);

    @Retryable
    String downloadRssXmlFile(@Nonnull String url);

}
