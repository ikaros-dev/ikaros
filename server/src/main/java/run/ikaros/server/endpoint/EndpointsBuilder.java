package run.ikaros.server.endpoint;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

public interface EndpointsBuilder<E extends Endpoint, B extends EndpointsBuilder> {
    B add(E endpoint);

    RouterFunction<ServerResponse> build();
}
