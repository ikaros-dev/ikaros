package run.ikaros.server.core.subject.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.infra.exception.subject.NoAvailableSubjectPlatformSynchronizerException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.service.SubjectSyncService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class SubjectSyncEndpoint implements CoreEndpoint {
    private final SubjectSyncService service;

    public SubjectSyncEndpoint(SubjectSyncService service) {
        this.service = service;
    }


    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/subject/sync";
        return SpringdocRouteBuilder.route()
            .POST("/subject/sync/platform", this::sync,
                builder -> builder.operationId("SyncSubjectAndPlatform")
                    .tag(tag)
                    .description("Sync subject and platform by platform name and platform id, "
                        + "create subject when params not contain subject id, "
                        + "update exists subject when params contain subject id.")
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .description("Subject id.")
                        .required(false)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("platform")
                        .description("Platform.")
                        .required(true)
                        .implementation(SubjectSyncPlatform.class))
                    .parameter(parameterBuilder()
                        .name("platformId")
                        .description("Platform id")
                        .required(true)
                        .implementation(String.class))
                    .response(Builder.responseBuilder()
                        .implementation(Subject.class))
            )

            .GET("/subject/syncs/subjectId/{id}", this::findSubjectSyncsBySubjectId,
                builder -> builder.operationId("GetSubjectSyncsBySubjectId")
                    .tag(tag).description("Get subject syncs by subject id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Subject id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder()
                        .description("Subject syncs by subject id.")
                        .implementationArray(SubjectSync.class))
            )

            .GET("/subject/syncs/platform", this::getSubjectSyncsByPlatformAndPlatformId,
                builder -> builder.operationId("GetSubjectSyncsByPlatformAndPlatformId")
                    .tag(tag).description("Get subject syncs by subject id.")
                    .parameter(parameterBuilder()
                        .name("platform")
                        .description("Platform.")
                        .required(true)
                        .implementation(SubjectSyncPlatform.class))
                    .parameter(parameterBuilder()
                        .name("platformId")
                        .description("Platform id")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder()
                        .description("Subject syncs by subject id.")
                        .implementationArray(SubjectSync.class))
            )

            .build();
    }

    private Mono<ServerResponse> sync(ServerRequest request) {
        UUID subjectId = UuidV7Utils.fromString(request.queryParam("subjectId").orElse(""));

        Optional<String> platformOp = request.queryParam("platform");
        Assert.isTrue(platformOp.isPresent() && StringUtils.hasText(platformOp.get()),
            "'platform' must not blank and belong of SubjectSyncPlatform.");
        SubjectSyncPlatform platform = SubjectSyncPlatform.valueOf(platformOp.get());

        Optional<String> platformIdOp = request.queryParam("platformId");
        Assert.isTrue(platformIdOp.isPresent() && StringUtils.hasText(platformIdOp.get()),
            "'platformId' must has text.");
        String platformId = platformIdOp.get();


        return service.sync(subjectId, platform, platformId)
            .flatMap(subject -> ServerResponse.ok().bodyValue(subjectId))
            .onErrorResume(NoAvailableSubjectPlatformSynchronizerException.class,
                err -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .bodyValue("Subject platform sync fail for subjectId="
                        + subjectId + ", platform=" + platform.name()
                        + ", platformId=" + platformId
                        + ", exception message=" + err.getMessage()));
    }

    private Mono<ServerResponse> findSubjectSyncsBySubjectId(ServerRequest request) {
        UUID subjectId = UuidV7Utils.fromString(request.pathVariable("id"));
        return service.findSubjectSyncsBySubjectId(subjectId)
            .collectList()
            .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }

    private Mono<ServerResponse> getSubjectSyncsByPlatformAndPlatformId(ServerRequest request) {
        SubjectSyncPlatform platform = SubjectSyncPlatform.valueOf(
            request.queryParam("platform").orElse(SubjectSyncPlatform.BGM_TV.name()));
        Long platformId = Long.valueOf(request.queryParam("platformId").orElse("-1L"));
        return service.findSubjectSyncsByPlatformAndPlatformId(platform, String.valueOf(platformId))
            .collectList()
            .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }
}
