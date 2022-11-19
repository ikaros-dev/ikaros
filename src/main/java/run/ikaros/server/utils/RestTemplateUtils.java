package run.ikaros.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.openapi.NetworkRestController;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

public class RestTemplateUtils {

    private static Map<String, RestTemplate> restTemplateProxyMap = new HashMap<>();
    private static RestTemplate restTemplate;

    public static synchronized RestTemplate buildRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }

    public static synchronized RestTemplate buildHttpProxyRestTemplate(
        @Nonnull String httpProxyHost,
        @Nonnull String httpProxyPort) {
        AssertUtils.notBlank(httpProxyHost, "httpProxyHost");
        AssertUtils.notBlank(httpProxyPort, "httpProxyPort");

        String key = httpProxyHost + ":" + httpProxyPort;
        if (restTemplateProxyMap.containsKey(key)) {
            return restTemplateProxyMap.get(key);
        }

        InetSocketAddress inetSocketAddress =
            new InetSocketAddress(httpProxyHost, Integer.parseInt(httpProxyPort));
        Proxy proxy = new Proxy(Proxy.Type.HTTP, inetSocketAddress);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(5000);
        requestFactory.setConnectTimeout(5000);
        requestFactory.setProxy(proxy);
        RestTemplate rt = new RestTemplate(requestFactory);

        restTemplateProxyMap.put(key, rt);
        return rt;
    }

    public static boolean testProxyConnect(@Nonnull String httpProxyHost,
                                           @Nonnull String httpProxyPort) {
        AssertUtils.notBlank(httpProxyHost, "httpProxyHost");
        AssertUtils.notBlank(httpProxyPort, "httpProxyPort");

        RestTemplate restTemplate = buildHttpProxyRestTemplate(httpProxyHost, httpProxyPort);

        try {
            ResponseEntity<String> responseEntity =
                restTemplate.getForEntity("https://www.youtube.com/", String.class);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return true;
            }
        } catch (NestedRuntimeException exception) {
            Logger logger = LoggerFactory.getLogger(NetworkRestController.class);
            logger.warn("http proxy verify fail", exception);
        }

        return false;
    }
}
