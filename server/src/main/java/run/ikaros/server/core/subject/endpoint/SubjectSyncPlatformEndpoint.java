package run.ikaros.server.core.subject.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import java.util.Optional;
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
import run.ikaros.api.infra.exception.subject.NoAvailableSubjectPlatformSynchronizerException;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.emus.SubjectSyncAction;
import run.ikaros.server.core.subject.service.SubjectSyncPlatformService;
import run.ikaros.server.core.subject.vo.PostSubjectSyncCondition;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class SubjectSyncPlatformEndpoint implements CoreEndpoint {
    private final SubjectSyncPlatformService service;

    public SubjectSyncPlatformEndpoint(SubjectSyncPlatformService service) {
        this.service = service;
    }


    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/SubjectSyncPlatform";
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
                    .parameter(parameterBuilder()
                        .name("action")
                        .description("Sync action, such as PULL or MERGE, "
                            + "default is PULL "
                            + "PULL will override all subject meta info, "
                            + "MERGE will update meta info that absent.")
                        .implementation(SubjectSyncAction.class))
                    .response(Builder.responseBuilder()
                        .implementation(Subject.class))
            )
            .build();
    }

    private Mono<ServerResponse> sync(ServerRequest request) {
        Optional<String> subjectIdOp = request.queryParam("subjectId");
        Long subjectId = subjectIdOp.isPresent() && StringUtils.hasText(subjectIdOp.get())
            ? Long.valueOf(subjectIdOp.get()) : null;

        Optional<String> platformOp = request.queryParam("platform");
        Assert.isTrue(platformOp.isPresent() && StringUtils.hasText(platformOp.get()),
            "'platform' must not blank and belong of SubjectSyncPlatform.");
        SubjectSyncPlatform platform = SubjectSyncPlatform.valueOf(platformOp.get());

        Optional<String> platformIdOp = request.queryParam("platformId");
        Assert.isTrue(platformIdOp.isPresent() && StringUtils.hasText(platformIdOp.get()),
            "'platformId' must has text.");
        String platformId = platformIdOp.get();

        Optional<String> actionOp = request.queryParam("action");
        SubjectSyncAction subjectSyncAction =
            SubjectSyncAction.valueOf(actionOp.orElse(SubjectSyncAction.PULL.name()));

        PostSubjectSyncCondition condition = PostSubjectSyncCondition.builder()
            .subjectId(subjectId).platform(platform)
            .platformId(platformId).subjectSyncAction(subjectSyncAction)
            .build();

        return service.sync(condition)
            .flatMap(subject -> ServerResponse.ok().bodyValue(subject))
            .onErrorResume(NoAvailableSubjectPlatformSynchronizerException.class,
                err -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .bodyValue("Subject platform sync fail for subjectId="
                        + subjectId + ", platform=" + platform.name()
                        + ", platformId=" + platformId
                        + ", exception message=" + err.getMessage()));
    }
}
