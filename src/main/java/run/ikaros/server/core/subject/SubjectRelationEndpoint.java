package run.ikaros.server.core.subject;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.constant.OpenApiConst;

@Slf4j
@Component
public class SubjectRelationEndpoint implements CoreEndpoint {
    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/SubjectRelation";
        return SpringdocRouteBuilder.route()
            .GET("/subject-relation/{id}", this::getById,
                builder -> builder
                    .tag(tag)
                    .operationId("GetSubjectRelationById")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.PATH)
                        .description("Subject relation id")
                        .implementation(Long.class)
                        .required(true)))
            .build();
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        return ServerResponse.ok().build();
    }
}
