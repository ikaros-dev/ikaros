package run.ikaros.api.endpoint;


import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

public interface Endpoint {
    RouterFunction<ServerResponse> endpoint();
}
