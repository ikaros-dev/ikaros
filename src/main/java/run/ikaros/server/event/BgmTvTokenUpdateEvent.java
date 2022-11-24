package run.ikaros.server.event;

import org.springframework.context.ApplicationEvent;

/**
 * @see run.ikaros.server.enums.OptionBgmTv#ACCESS_TOKEN
 */
public class BgmTvTokenUpdateEvent extends ApplicationEvent {
    private final String accessToken;

    public BgmTvTokenUpdateEvent(Object source, String accessToken) {
        super(source);
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
