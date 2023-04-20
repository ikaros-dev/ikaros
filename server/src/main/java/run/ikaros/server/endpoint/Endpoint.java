package run.ikaros.server.endpoint;


import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

public interface Endpoint {
    RouterFunction<ServerResponse> endpoint();
}
