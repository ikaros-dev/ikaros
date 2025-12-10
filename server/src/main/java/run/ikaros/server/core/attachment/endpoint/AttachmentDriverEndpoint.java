package run.ikaros.server.core.attachment.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class AttachmentDriverEndpoint implements CoreEndpoint {
    private final AttachmentDriverService service;

    public AttachmentDriverEndpoint(AttachmentDriverService service) {
        this.service = service;
    }


    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/attachment/driver";
        return SpringdocRouteBuilder.route()
            .PUT("/attachment/driver", this::save,
                builder -> builder.operationId("SaveAttachmentDriver")
                    .tag(tag).description("Save attachment driver.")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(AttachmentDriver.class))
                    .response(responseBuilder().implementation(AttachmentDriver.class))
            )

            .PUT("/attachment/driver/enable/id/{id}", this::enableDriver,
                builder -> builder.operationId("EnableDriver")
                    .tag(tag).description("Enable attachment driver.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
            )

            .PUT("/attachment/driver/disable/id/{id}", this::disableDriver,
                builder -> builder.operationId("EnableDriver")
                    .tag(tag).description("Disable attachment driver.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
            )

            .DELETE("/attachment/driver/id/{id}", this::deleteById,
                builder -> builder.tag(tag)
                    .operationId("DeleteAttachmentDriverById")
                    .description("Delete attachment driver by ID.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
            )

            .GET("/attachment/driver/id/{id}", this::getById,
                builder -> builder.tag(tag)
                    .operationId("GetAttachmentDriverById")
                    .description("Get attachment driver by ID.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder().implementation(AttachmentDriver.class))
            )

            .DELETE("/attachment/driver/uk", this::deleteByTypeAndName,
                builder -> builder.operationId("DeleteByTypeAndName")
                    .tag(tag).description("Delete attachment driver by type and name.")
                    .parameter(parameterBuilder().name("type")
                        .description("AttachmentDriver type")
                        .required(true)
                        .implementation(AttachmentDriverType.class))
                    .parameter(parameterBuilder().name("name")
                        .description("AttachmentDriver name")
                        .required(true)
                        .implementation(String.class))
            )

            .GET("/attachment/driver/uk", this::getByTypeAndName,
                builder -> builder.operationId("GetByTypeAndName")
                    .tag(tag).description("Get attachment driver by type and name.")
                    .parameter(parameterBuilder().name("type")
                        .description("AttachmentDriver type")
                        .required(true)
                        .implementation(AttachmentDriverType.class))
                    .parameter(parameterBuilder().name("name")
                        .description("AttachmentDriver name")
                        .required(true)
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementation(AttachmentDriver.class))
            )

            .GET("/attachments/driver/condition", this::listByCondition,
                builder -> builder.operationId("ListAttachmentsByCondition")
                    .tag(tag).description("List attachments by condition.")
                    .parameter(parameterBuilder()
                        .name("page")
                        .description("第几页，从1开始, 默认为1.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("size")
                        .description("每页条数，默认为10.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("name")
                        .description("经过Basic64编码的附件名称，附件名称字段模糊查询。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("parentId")
                        .description("附件的父附件ID，父附件一般时目录类型。"))
                    .parameter(parameterBuilder()
                        .name("refresh")
                        .description("是否从驱动拉取最新数据,默认false.如果为false可能拉取的不是最新的数据，"
                            + "可通过此参数设置未true在查询前刷新数据，操作比较耗时不推荐，"
                            + "更推荐通过刷新接口去主动刷新数据。")
                        .example("false")
                        .implementation(Boolean.class))
                    .response(responseBuilder().implementation(PagingWrap.class))
            )
            .build();
    }

    private Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(AttachmentDriver.class)
            .flatMap(service::save)
            .flatMap(attachmentDriver -> ServerResponse.ok().bodyValue(attachmentDriver));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.removeById(Long.parseLong(id))
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> deleteByTypeAndName(ServerRequest request) {
        String name = request.queryParam("name").orElse("-1");
        String type = request.queryParam("type").orElse(null);
        return service.removeByTypeAndName(type, name)
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.findById(Long.parseLong(id))
            .flatMap(driver -> ServerResponse.ok().bodyValue(driver));
    }

    private Mono<ServerResponse> getByTypeAndName(ServerRequest request) {
        String name = request.queryParam("name").orElse("-1");
        String type = request.queryParam("type").orElse(null);
        return service.findByTypeAndName(type, name)
            .flatMap(driver -> ServerResponse.ok().bodyValue(driver));
    }

    private Mono<ServerResponse> enableDriver(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.enable(Long.valueOf(id))
            .then(ServerResponse.ok()
                .bodyValue("Enable success"));
    }

    private Mono<ServerResponse> disableDriver(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.disable(Long.valueOf(id))
            .then(ServerResponse.ok()
                .bodyValue("Disable success"));
    }

    private Mono<ServerResponse> listByCondition(ServerRequest request) {
        Optional<String> pageOp = request.queryParam("page");
        if (pageOp.isEmpty()) {
            pageOp = Optional.of("1");
        }
        final Integer page = Integer.valueOf(pageOp.get());

        Optional<String> sizeOp = request.queryParam("size");
        if (sizeOp.isEmpty()) {
            sizeOp = Optional.of("10");
        }
        final Integer size = Integer.valueOf(sizeOp.get());

        Optional<String> nameOp = request.queryParam("name");
        final String name = nameOp.isPresent() && StringUtils.hasText(nameOp.get())
            ? new String(Base64.getDecoder().decode(nameOp.get()), StandardCharsets.UTF_8)
            : "";

        Optional<String> parentIdOp = request.queryParam("parentId");
        Long parentId = parentIdOp.isPresent() ? Long.parseLong(parentIdOp.get()) : null;

        final Boolean refresh =
            Boolean.valueOf(request.queryParam("refresh").orElse("false"));

        return Mono.just(AttachmentSearchCondition.builder()
                .page(page).size(size).refresh(refresh)
                .name(name).parentId(parentId)
                .build())
            .flatMap(service::listEntitiesByCondition)
            .flatMap(pagingWrap -> ServerResponse.ok().bodyValue(pagingWrap));
    }

}
