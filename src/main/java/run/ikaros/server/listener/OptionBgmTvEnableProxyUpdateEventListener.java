package run.ikaros.server.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.event.BgmTvHttpProxyUpdateEvent;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.RestTemplateUtils;

import jakarta.annotation.Nonnull;

@Component
public class OptionBgmTvEnableProxyUpdateEventListener implements
    ApplicationListener<BgmTvHttpProxyUpdateEvent> {

    private final BgmTvService bgmTvService;

    public OptionBgmTvEnableProxyUpdateEventListener(BgmTvService bgmTvService) {
        this.bgmTvService = bgmTvService;
    }

    @Override
    public void onApplicationEvent(@Nonnull BgmTvHttpProxyUpdateEvent event) {
        AssertUtils.notNull(event, "OptionBgmTvEnableProxyUpdateEvent");

        Boolean enable = event.getEnable();
        String httpProxyHost = event.getHttpProxyHost();
        Integer httpProxyPort = event.getHttpProxyPort();

        RestTemplate restTemplate = null;
        if (enable) {
            restTemplate =
                RestTemplateUtils.buildHttpProxyRestTemplate(httpProxyHost, httpProxyPort);
        } else {
            restTemplate = RestTemplateUtils.buildRestTemplate();
        }
        bgmTvService.setRestTemplate(restTemplate);
    }
}
