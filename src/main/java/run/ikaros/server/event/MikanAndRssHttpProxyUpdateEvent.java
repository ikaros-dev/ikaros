package run.ikaros.server.event;

import org.springframework.context.ApplicationEvent;

/**
 * @see run.ikaros.server.enums.OptionMikan#ENABLE_PROXY
 */
public class MikanAndRssHttpProxyUpdateEvent extends ApplicationEvent {

    private final Boolean enable;
    private final String httpProxyHost;
    private final String httpProxyPort;

    public MikanAndRssHttpProxyUpdateEvent(Object source, Boolean enable, String httpProxyHost,
                                           String httpProxyPort) {
        super(source);
        this.enable = enable;
        this.httpProxyHost = httpProxyHost;
        this.httpProxyPort = httpProxyPort;
    }

    public Boolean getEnable() {
        return enable;
    }

    public String getHttpProxyHost() {
        return httpProxyHost;
    }

    public String getHttpProxyPort() {
        return httpProxyPort;
    }
}
