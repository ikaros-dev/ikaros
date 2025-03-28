package run.ikaros.server.core.collection;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class CollectionEndpoint implements CoreEndpoint {
    private final CollectionService collectionService;

    public CollectionEndpoint(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/collection";
        return SpringdocRouteBuilder.route()

            .GET("/collection/type/subjectId/{id}", this::getTypeBySubjectId,
                builder -> builder.operationId("GetTypeBySubjectId")
                    .tag(tag).description("Get collection type by subject id.")
                    .parameter(parameterBuilder()
                        .name("id").required(true).description("Subject id")
                        .in(ParameterIn.PATH).implementation(Long.class))
                    .response(responseBuilder()
                        .description("collection type.")
                        .implementation(CollectionType.class))
            )

            .build();
    }

    private Mono<ServerResponse> getTypeBySubjectId(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        Long subjectId = Long.parseLong(id);
        return collectionService.findTypeBySubjectId(subjectId)
            .flatMap(collectionType -> ServerResponse.ok().bodyValue(collectionType));
    }
}
