package run.ikaros.server.core.statics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class StaticEndpoint implements CoreEndpoint {
    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return null;
    }
}
