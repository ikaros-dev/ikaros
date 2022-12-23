package run.ikaros.server.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.event.BgmTvTokenUpdateEvent;
import run.ikaros.server.utils.AssertUtils;

import jakarta.annotation.Nonnull;
import run.ikaros.server.utils.StringUtils;

@Slf4j
@Component
public class OptionBgmTvTokenUpdateEventListener
    implements ApplicationListener<BgmTvTokenUpdateEvent> {

    private final BgmTvService bgmTvService;

    public OptionBgmTvTokenUpdateEventListener(BgmTvService bgmTvService) {
        this.bgmTvService = bgmTvService;
    }

    @Override
    public void onApplicationEvent(@Nonnull BgmTvTokenUpdateEvent event) {
        AssertUtils.notNull(event, "event");
        log.debug("receive BgmTvTokenUpdateEvent");
        String accessToken = event.getAccessToken();
        if (StringUtils.isNotBlank(accessToken)) {
            bgmTvService.refreshHttpHeaders(accessToken);
        }
    }
}
