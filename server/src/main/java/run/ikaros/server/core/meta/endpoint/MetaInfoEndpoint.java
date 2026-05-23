package run.ikaros.server.core.meta.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.subject.SubjectRecord;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.meta.MetaInfoService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class MetaInfoEndpoint implements CoreEndpoint {

    private final MetaInfoService service;

    public MetaInfoEndpoint(MetaInfoService service) {
        this.service = service;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/metaInfo";
        return SpringdocRouteBuilder.route()

            .GET("/metaInfo/search", this::searchSubjects,
                builder -> builder.operationId("SearchMetaInfoSubjects")
                    .tag(tag)
                    .description("Search subjects from third-party metadata platform by keyword.")
                    .parameter(parameterBuilder()
                        .name("platform")
                        .description("Platform.")
                        .required(true)
                        .implementation(SubjectSyncPlatform.class))
                    .parameter(parameterBuilder()
                        .name("keyword")
                        .description("Search keyword.")
                        .required(true)
                        .implementation(String.class))
                    .response(Builder.responseBuilder()
                        .description("Matched subject records.")
                        .implementationArray(SubjectRecord.class))
            )

            .GET("/metaInfo/subject", this::getSubject,
                builder -> builder.operationId("GetMetaInfoSubjectByPlatformId")
                    .tag(tag)
                    .description("Get subject record from third-party metadata platform "
                        + "by platform id.")
                    .parameter(parameterBuilder()
                        .name("platform")
                        .description("Platform.")
                        .required(true)
                        .implementation(SubjectSyncPlatform.class))
                    .parameter(parameterBuilder()
                        .name("platformId")
                        .description("Platform id.")
                        .required(true)
                        .implementation(String.class))
                    .response(Builder.responseBuilder()
                        .description("Subject record with episodes, tags and syncs.")
                        .implementation(SubjectRecord.class))
            )

            .build();
    }

    private Mono<ServerResponse> searchSubjects(ServerRequest request) {
        SubjectSyncPlatform platform = parsePlatform(request);
        String keyword = parseRequiredParam(request, "keyword");
        return service.searchSubjects(platform, keyword)
            .collectList()
            .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }

    private Mono<ServerResponse> getSubject(ServerRequest request) {
        SubjectSyncPlatform platform = parsePlatform(request);
        String platformId = parseRequiredParam(request, "platformId");
        return service.getSubjectByPlatformId(platform, platformId)
            .flatMap(record -> ServerResponse.ok().bodyValue(record));
    }

    private SubjectSyncPlatform parsePlatform(ServerRequest request) {
        Optional<String> platformOp = request.queryParam("platform");
        Assert.isTrue(platformOp.isPresent() && StringUtils.hasText(platformOp.get()),
            "'platform' must not blank and belong of SubjectSyncPlatform.");
        return SubjectSyncPlatform.valueOf(platformOp.get());
    }

    private String parseRequiredParam(ServerRequest request, String name) {
        Optional<String> paramOp = request.queryParam(name);
        Assert.isTrue(paramOp.isPresent() && StringUtils.hasText(paramOp.get()),
            "'" + name + "' must not blank.");
        return paramOp.get();
    }
}
