package run.ikaros.server.endpoint;


import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

public interface CustomEndpoint {
    RouterFunction<ServerResponse> endpoint();
}
