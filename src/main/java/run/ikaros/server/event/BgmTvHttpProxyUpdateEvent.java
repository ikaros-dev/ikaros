package run.ikaros.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @see run.ikaros.server.enums.OptionBgmTv#ENABLE_PROXY
 */
@Getter
public class BgmTvHttpProxyUpdateEvent extends ApplicationEvent {

    private final Boolean enable;
    private final String httpProxyHost;
    private final Integer httpProxyPort;
    private final Integer readTimeout;
    private final Integer connectTimeout;

    public BgmTvHttpProxyUpdateEvent(Object source, Boolean enable, String httpProxyHost,
                                     Integer httpProxyPort, Integer readTimeout,
                                     Integer connectTimeout) {
        super(source);
        this.enable = enable;
        this.httpProxyHost = httpProxyHost;
        this.httpProxyPort = httpProxyPort;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
    }

}
