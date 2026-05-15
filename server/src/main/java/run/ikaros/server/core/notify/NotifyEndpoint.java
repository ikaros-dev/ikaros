package run.ikaros.server.core.notify;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class NotifyEndpoint implements CoreEndpoint {
    private final NotifyService notifyService;

    public NotifyEndpoint(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/notify";
        return SpringdocRouteBuilder.route()
            .POST("/notify/mail/test", this::testMailSend,
                builder -> builder.operationId("TestMailSend")
                    .tag(tag).description("Test mail send"))

            .build();
    }

    private Mono<ServerResponse> testMailSend(ServerRequest request) {
        return notifyService.testMail()
            .then(ServerResponse.ok().build());
    }
}
