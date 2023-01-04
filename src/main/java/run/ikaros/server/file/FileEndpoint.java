package run.ikaros.server.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.server.endpoint.CustomEndpoint;

@Slf4j
@Component
public class FileEndpoint implements CustomEndpoint {

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return null;
    }
}
