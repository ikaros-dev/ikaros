package run.ikaros.api.endpoint;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

public interface Endpoint {
    RouterFunction<ServerResponse> endpoint();
}
