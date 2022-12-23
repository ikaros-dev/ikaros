package run.ikaros.server.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.core.service.RssService;
import run.ikaros.server.event.MikanAndRssHttpProxyUpdateEvent;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.RestTemplateUtils;

import jakarta.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Slf4j
@Component
public class OptionMikanEnableProxyUpdateEventListener implements
    ApplicationListener<MikanAndRssHttpProxyUpdateEvent> {

    private final MikanService mikanService;
    private final RssService rssService;

    public OptionMikanEnableProxyUpdateEventListener(MikanService mikanService,
                                                     RssService rssService) {
        this.mikanService = mikanService;
        this.rssService = rssService;
    }

    @Override
    public void onApplicationEvent(@Nonnull MikanAndRssHttpProxyUpdateEvent event) {
        AssertUtils.notNull(event, "OptionMikanEnableProxyUpdateEvent");
        log.debug("receive MikanAndRssHttpProxyUpdateEvent");

        Boolean enable = event.getEnable();
        String httpProxyHost = event.getHttpProxyHost();
        Integer httpProxyPort = event.getHttpProxyPort();

        RestTemplate restTemplate;
        Proxy proxy = null;
        if (enable) {
            restTemplate =
                RestTemplateUtils.buildHttpProxyRestTemplate(httpProxyHost, httpProxyPort);
            proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(httpProxyHost, httpProxyPort));
        } else {
            restTemplate = RestTemplateUtils.buildRestTemplate();
        }
        mikanService.setRestTemplate(restTemplate);
        rssService.setProxy(proxy);
    }
}
