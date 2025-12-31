package run.ikaros.server.core.tag;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.tag.AttachmentTag;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class TagEndpoint implements CoreEndpoint {
    private final TagService tagService;
    private final UserService userService;

    public TagEndpoint(TagService tagService, UserService userService) {
        this.tagService = tagService;
        this.userService = userService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/tag";
        return SpringdocRouteBuilder.route()
            .GET("/tags/condition", this::listByCondition,
                builder -> builder.operationId("ListTagsByCondition")
                    .tag(tag).description("List tags by condition.")
                    .parameter(parameterBuilder()
                        .name("type").required(false)
                        .implementation(TagType.class))
                    .parameter(parameterBuilder()
                        .name("masterId").required(false)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("userId").required(false)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("name").required(false)
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementationArray(Tag.class)))

            .GET("/tags/subject/subjectId/{subjectId}", this::listSubjectTagsBySubjectId,
                builder -> builder.operationId("ListSubjectTagsBySubjectId")
                    .tag(tag).description("List subject tags by subject id.")
                    .parameter(parameterBuilder().name("subjectId").required(true)
                        .in(ParameterIn.PATH).implementation(Long.class))
                    .response(responseBuilder().implementationArray(SubjectTag.class)))

            .GET("/tags/attachment/attachmentId/{attachmentId}",
                this::listAttachmentTagsByAttachmentId,
                builder -> builder.operationId("ListAttachmentTagsByAttachmentId")
                    .tag(tag).description("List attachment tags by attachment id.")
                    .parameter(parameterBuilder().name("attachmentId").required(true)
                        .in(ParameterIn.PATH).implementation(Long.class))
                    .response(responseBuilder().implementationArray(AttachmentTag.class)))

            .POST("/tag", this::create,
                builder -> builder.operationId("CreateTag")
                    .tag(tag).description("Create tag")
                    .requestBody(requestBodyBuilder().implementation(Tag.class))
                    .response(responseBuilder().implementation(Tag.class)))

            .DELETE("/tag/id/{id}", this::removeById,
                builder -> builder.operationId("RemoveTagById")
                    .tag(tag).description("Remove tag by id.")
                    .parameter(parameterBuilder().name("id")
                        .required(true).in(ParameterIn.PATH)
                        .implementation(Long.class)))

            .DELETE("/tag/condition", this::removeByCondition,
                builder -> builder.operationId("RemoveTagByCondition")
                    .tag(tag).description("Remove tag by condition.")
                    .parameter(parameterBuilder()
                        .name("type").required(false)
                        .implementation(TagType.class))
                    .parameter(parameterBuilder()
                        .name("masterId").required(false)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("name").required(false)
                        .implementation(String.class)))

            .build();
    }

    private Mono<ServerResponse> listByCondition(ServerRequest request) {
        Optional<String> typeOp = request.queryParam("type");
        TagType type = null;
        if (typeOp.isPresent() && StringUtils.isNotBlank(typeOp.get())) {
            type = TagType.valueOf(typeOp.get());
        }

        UUID masterId = UuidV7Utils.fromString(request.queryParam("masterId").orElse(""));
        UUID userId = UuidV7Utils.fromString(request.queryParam("userId").orElse(""));

        Optional<String> nameOp = request.queryParam("name");
        String name = nameOp.orElse(null);

        return tagService.findAll(type, masterId, userId, name)
            .collectList()
            .flatMap(tags -> ServerResponse.ok().bodyValue(tags));
    }

    private Mono<ServerResponse> listSubjectTagsBySubjectId(ServerRequest request) {
        UUID subjectId = UuidV7Utils.fromString(request.pathVariable("subjectId"));
        return tagService.findSubjectTags(subjectId)
            .collectList()
            .flatMap(subjectTags -> ServerResponse.ok()
                .bodyValue(subjectTags));
    }


    private Mono<ServerResponse> listAttachmentTagsByAttachmentId(ServerRequest request) {
        UUID attachmentId = UuidV7Utils.fromString(request.pathVariable("attachmentId"));
        return tagService.findAttachmentTags(attachmentId)
            .collectList()
            .flatMap(attachmentTags -> ServerResponse.ok()
                .bodyValue(attachmentTags));
    }

    private Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(Tag.class)
            .flatMap(tag -> request.principal()
                .map(Principal::getName)
                .flatMap(userService::getUserByUsername)
                .map(user -> user.entity().getId())
                .map(tag::setUserId))
            .flatMap(tagService::create)
            .flatMap(tag -> ServerResponse.ok().bodyValue(tag));
    }

    private Mono<ServerResponse> removeById(ServerRequest request) {
        UUID id = UuidV7Utils.fromString(request.pathVariable("id"));
        return tagService.removeById(id)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> removeByCondition(ServerRequest request) {
        Optional<String> typeOp = request.queryParam("type");
        Optional<String> nameOp = request.queryParam("name");
        Assert.isTrue(typeOp.isPresent(), "'type' must has value.");
        Assert.isTrue(nameOp.isPresent(), "'name' must has value.");

        TagType type = TagType.valueOf(typeOp.get());
        String name = nameOp.get();

        UUID masterId = UuidV7Utils.fromString(request.queryParam("masterId").orElse(""));
        Assert.notNull(masterId, "'masterId' must not null.");
        return tagService.remove(type, masterId, name)
            .then(ServerResponse.ok().build());
    }

}
