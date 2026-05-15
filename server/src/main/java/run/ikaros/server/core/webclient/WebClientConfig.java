package run.ikaros.server.core.webclient;

import org.springframework.boot.http.codec.autoconfigure.HttpCodecsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    /**
     * Ikaros web client.
     *
     * @return {@link WebClient} instance
     * @see HttpCodecsProperties#getMaxInMemorySize()
     */
    @Bean
    public WebClient webClient(HttpCodecsProperties codecProperties) {
        DataSize maxInMemorySize = codecProperties.getMaxInMemorySize();
        maxInMemorySize = maxInMemorySize == null ? DataSize.ofMegabytes(100) : maxInMemorySize;
        long bytes = maxInMemorySize.toBytes();
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize((int) bytes))
            .build();
        return WebClient.builder()
            .exchangeStrategies(strategies)
            .defaultHeader(HttpHeaders.USER_AGENT, WebClientConst.REST_TEMPLATE_USER_AGENT)
            .build();
    }
}
