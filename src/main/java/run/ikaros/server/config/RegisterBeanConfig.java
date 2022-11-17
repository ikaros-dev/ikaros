package run.ikaros.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.constants.HttpConst;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static run.ikaros.server.constants.HttpConst.HTTP_PROXY_HOST;
import static run.ikaros.server.constants.HttpConst.HTTP_PROXY_PORT;

/**
 * @author guohao
 * @date 2022/10/20
 */
@Configuration
public class RegisterBeanConfig {

    @Bean
    public RestTemplate restTemplate() {
        Proxy proxy =
            new Proxy(Proxy.Type.HTTP, new InetSocketAddress(HTTP_PROXY_HOST, HTTP_PROXY_PORT));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(proxy);
        return new RestTemplate(requestFactory);
    }


}
