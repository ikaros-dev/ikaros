package run.ikaros.server.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.event.BgmTvTokenUpdateEvent;
import run.ikaros.server.utils.AssertUtils;

import jakarta.annotation.Nonnull;

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
        String accessToken = event.getAccessToken();
        bgmTvService.refreshHttpHeaders(accessToken);
    }
}
