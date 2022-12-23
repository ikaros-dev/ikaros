package run.ikaros.server.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.ikaros.server.event.OptionNetworkUpdateEvent;
import run.ikaros.server.model.dto.OptionNetworkDTO;
import run.ikaros.server.tripartite.dmhy.DmhyClient;
import run.ikaros.server.utils.StringUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Slf4j
@Component
public class OptionNetworkUpdateEventListener
    implements ApplicationListener<OptionNetworkUpdateEvent> {

    private final DmhyClient dmhyClient;

    public OptionNetworkUpdateEventListener(DmhyClient dmhyClient) {
        this.dmhyClient = dmhyClient;
    }

    @Override
    public void onApplicationEvent(OptionNetworkUpdateEvent event) {
        log.debug("receive OptionNetworkUpdateEvent");
        OptionNetworkDTO optionNetworkDTO = event.getOptionNetworkDTO();
        refreshDmhyClientProxy(optionNetworkDTO);
    }

    private void refreshDmhyClientProxy(OptionNetworkDTO optionNetworkDTO) {
        String proxyHttpHost = optionNetworkDTO.getProxyHttpHost();
        Integer proxyHttpPort = optionNetworkDTO.getProxyHttpPort();
        if (StringUtils.isNotBlank(proxyHttpHost) && proxyHttpPort != null) {
            Proxy proxy =
                new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHttpHost, proxyHttpPort));
            dmhyClient.setProxy(proxy);
        }
    }
}
