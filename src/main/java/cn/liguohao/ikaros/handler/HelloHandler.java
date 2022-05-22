package cn.liguohao.ikaros.handler;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author li-guohao
 */
@Component
public class HelloHandler {

    private static final String MSG = "Hello Ikaros!";

    public Mono<ServerResponse> hello(ServerRequest request) {
        return ok().contentType(MediaType.TEXT_PLAIN)
            .body(Mono.justOrEmpty(MSG), String.class);
    }

}
