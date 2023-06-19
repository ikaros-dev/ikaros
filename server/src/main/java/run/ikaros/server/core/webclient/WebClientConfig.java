package run.ikaros.server.core.webclient;

import org.springframework.boot.autoconfigure.codec.CodecProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@EnableRetry
@Configuration
public class WebClientConfig {
    /**
     * Ikaros web client.
     *
     * @return {@link WebClient} instance
     * @see CodecProperties#getMaxInMemorySize()
     */
    @Bean
    public WebClient webClient(CodecProperties codecProperties) {
        DataSize maxInMemorySize = codecProperties.getMaxInMemorySize();
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
