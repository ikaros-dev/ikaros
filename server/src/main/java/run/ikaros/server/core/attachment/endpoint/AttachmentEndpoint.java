package run.ikaros.server.core.attachment.endpoint;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static run.ikaros.api.core.attachment.AttachmentConst.V_ROOT_DIRECTORY_PARENT_ID;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.core.attachment.exception.AttachmentParentNotFoundException;
import run.ikaros.api.core.attachment.exception.AttachmentUploadException;
import run.ikaros.api.core.media.MediaFileFormatHint;
import run.ikaros.api.core.media.MediaFilePolicy;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.utils.DataBufferUtils;

@Slf4j
@Component
public class AttachmentEndpoint implements CoreEndpoint {
    private final AttachmentService attachmentService;
    /** 附件名称和真实媒体格式验证服务。 */
    private final AttachmentMediaValidationService mediaValidationService;

    public AttachmentEndpoint(AttachmentService attachmentService,
                              AttachmentMediaValidationService mediaValidationService) {
        this.attachmentService = attachmentService;
        this.mediaValidationService = mediaValidationService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/attachment";
        return SpringdocRouteBuilder.route()

            .POST("/attachment/upload", contentType(MediaType.MULTIPART_FORM_DATA), this::upload,
                builder -> builder
                    .operationId("UploadAttachment")
                    .tag(tag)
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().implementation(FilePartEvent.class))
                        ))
                    .response(responseBuilder().implementation(Attachment.class))
                    .build())

            .GET("/attachments/condition", this::listByCondition,
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
                        .name("type")
                        .description("附件类型。")
                        .implementation(AttachmentType.class))
                    .parameter(parameterBuilder()
                        .name("name")
                        .description("经过Basic64编码的附件名称，附件名称字段模糊查询。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("parentId")
                        .description("附件的父附件ID，父附件一般时目录类型。"))
                    .response(responseBuilder().implementation(PagingWrap.class))
            )

            .GET("/attachment/media-formats", this::listMediaFormats,
                builder -> builder.operationId("ListAttachmentMediaFormats")
                    .tag(tag)
                    .description("获取服务端媒体格式白名单提示。该结果仅用于客户端展示，"
                        + "上传文件仍由服务端执行名称门禁和真实格式检测。")
                    .response(responseBuilder().responseCode("200")
                        .description("返回由服务端权威媒体格式枚举生成的全部格式提示。")
                        .implementationArray(MediaFileFormatHint.class)))

            .GET("/attachment/{id}", this::getById,
                builder -> builder.operationId("GetAttachmentById").tag(tag)
                    .parameter(parameterBuilder().name("id")
                        .description("Attachment ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class))
                    .response(responseBuilder().implementation(Attachment.class)))

            .GET("/attachment/paths/{id}", this::getAttachmentPathDirsById,
                builder -> builder.operationId("GetAttachmentPathDirsById")
                    .tag(tag).description("Get attachment path dirs by id.")
                    .parameter(parameterBuilder()
                        .name("id").description("Attachment id.")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class))
                    .response(responseBuilder().implementationArray(Attachment.class)))

            .DELETE("/attachment/{id}", this::deleteById,
                builder -> builder.operationId("DeleteAttachment").tag(tag)
                    .parameter(parameterBuilder().name("id")
                        .description("Attachment ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)))

            .POST("/attachment/directory", this::createDirectory,
                builder -> builder.operationId("CreateDirectory")
                    .tag(tag).description("Create directory")
                    .parameter(parameterBuilder()
                        .name("name").required(true)
                        .description("经过Basic64编码的附件名称，附件名称字段模糊查询。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("parentId")
                        .description("附件的父附件ID，父附件一般时目录类型。"))
                    .response(responseBuilder().implementationArray(Attachment.class)))

            .PUT("/attachment/update", this::update,
                builder -> builder.operationId("UpdateAttachment")
                    .tag(tag).description("Update attachment.")
                    .requestBody(Builder.requestBodyBuilder().implementation(Attachment.class))
            )

            // Large multipart attachment fragment upload support
            .POST("/attachment/fragment/unique", this::generateFragmentUploadAttachmentUniqueId,
                builder -> builder.operationId("GenerateFragmentUploadAttachmentUniqueId")
                    .tag(tag).description("Generate fragment upload attachment unique id.")
                    .response(responseBuilder()
                        .description("Random uuid.")
                        .implementation(String.class)))
            .PATCH("/attachment/fragment/patch/{unique}",
                this::receiveFragmentUploadChunkAttachment,
                builder -> builder.operationId("ReceiveFragmentUploadChunkAttachment")
                    .tag(tag).description("Receive fragment upload chunk attachment.")
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("unique").required(true)
                        .description("Chunk attachment unique id."))
                    .parameter(parameterBuilder()
                        .name("PARENT-ID").in(ParameterIn.HEADER)
                        .description("附件的父附件ID，父附件一般时目录类型。"))
                    .parameter(parameterBuilder().in(ParameterIn.HEADER)
                        .name("Upload-Length").required(true)
                        .description("Upload chunk attachment length."))
                    .parameter(parameterBuilder().in(ParameterIn.HEADER)
                        .name("Upload-Offset").required(true)
                        .description("Upload chunk attachment offset."))
                    .parameter(parameterBuilder().in(ParameterIn.HEADER)
                        .name("Upload-Name").required(true)
                        .description("Upload chunk attachment file name.")))
            .DELETE("/attachment/fragment/revert", this::revertFragmentUploadAttachmentByUnique,
                builder -> builder.operationId("RevertFragmentUploadAttachmentByUnique")
                    .tag(tag).description("Revert fragment upload attachment by unique id.")
                    .requestBody(Builder.requestBodyBuilder()
                        .description("Unique id.")
                        .implementation(String.class)))

            .GET("/attachment/url/download/id/{id}", this::getDownloadUrl,
                builder -> builder.operationId("GetDownloadUrl")
                    .tag(tag).description("Get download url.")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("id")
                        .description("Download url.")
                        .implementation(String.class))
                    .response(responseBuilder().implementation(String.class)))

            .GET("/attachment/url/read/id/{id}", this::getReadUrl,
                builder -> builder.operationId("GetReadUrl")
                    .tag(tag).description("Get read url.")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("id")
                        .description("Read url.")
                        .implementation(String.class))
                    .response(responseBuilder().implementation(String.class)))

            .GET("/attachment/{id}/url/conditions", this::getUrlConditions,
                builder -> builder.operationId("GetUrlConditions")
                    .tag(tag).description("Get URL conditions for attachment's driver.")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH).name("id")
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementationArray(run.ikaros.api.core.attachment
                            .AccessUrlCondition.class)))

            .POST("/attachment/url/with/conditions", this::postUrlWithConditions,
                builder -> builder.operationId("PostUrlWithConditions")
                    .tag(tag).description("Get attachment URL with conditions.")
                    .requestBody(org.springdoc.core.fn.builders.requestbody
                        .Builder.requestBodyBuilder()
                        .required(true)
                        .implementation(UrlWithConditionsRequest.class))
                    .response(responseBuilder()
                        .implementation(String.class)))

            .GET("/attachment/stream/id/{id}", this::getStreamById,
                builder -> builder.operationId("GetStreamById")
                    .tag(tag).description("Get attachment stream by id.")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("id")
                        .description("Attachment id.")
                        .implementation(String.class))
                    .response(responseBuilder().implementation(String.class)))

            .GET("/attachment/svg-preview/id/{id}", this::getSvgPreviewById,
                builder -> builder.operationId("GetSvgPreviewById")
                    .tag(tag)
                    .description("在隔离策略下预览指定的 SVG 附件，仅允许读取 SVG 文件。")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("id")
                        .description("待预览 SVG 附件的标识。")
                        .required(true)
                        .implementation(UUID.class))
                    .response(responseBuilder().responseCode("200")
                        .description("附件存在且为 SVG，返回带固定安全响应头的文件流。")
                        .implementation(String.class))
                    .response(responseBuilder().responseCode("404")
                        .description("附件不存在或附件不是 SVG 文件。")
                        .implementation(String.class)))

            .build();
    }

    /** 仅供 SpringDoc 描述 multipart 文件字段的公共请求模型。 */
    public interface UploadRequest {

        /**
         * 获取 multipart 文件字段。
         *
         * @return 上传文件字段
         */
        @Schema(requiredMode = REQUIRED, description = "文件")
        FilePart getFile();
    }

    private Mono<ServerResponse> upload(ServerRequest request) {
        return request.body(BodyExtractors.toFlux(PartEvent.class))
            .doOnDiscard(PartEvent.class, AttachmentEndpoint::releasePartEvent)
            .windowUntil(PartEvent::isLast)
            .index()
            .concatMap(indexedWindow -> indexedWindow.getT2()
                .switchOnFirst((signal, partEvents) -> {
                    if (!signal.hasValue()) {
                        return partEvents.then(Mono.empty());
                    }
                    PartEvent firstEvent = signal.get();
                    if (indexedWindow.getT1() != 0 || !(firstEvent instanceof FilePartEvent file)
                        || !"file".equals(file.name())) {
                        return releasePartEvents(partEvents)
                            .then(Mono.error(new ServerWebInputException("无效的文件分段")));
                    }
                    try {
                        mediaValidationService.validateFilename(file.filename());
                    } catch (IllegalArgumentException exception) {
                        return releasePartEvents(partEvents)
                            .then(Mono.error(new ServerWebInputException(exception.getMessage(),
                                null, exception)));
                    }
                    Flux<DataBuffer> content = partEvents.map(PartEvent::content)
                        .doOnDiscard(DataBuffer.class, org.springframework.core.io.buffer
                            .DataBufferUtils::release);
                    return mediaValidationService.validate(content, file.filename())
                        .flatMap(validated -> attachmentService.upload(
                            AttachmentUploadCondition.builder()
                                .name(file.filename())
                                .dataBufferFlux(validated.content())
                                .build()));
                }))
            .single()
            // Response upload file data
            .flatMap(file -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(file))
            .onErrorResume(NotFoundException.class, e -> ServerResponse.from(
                ErrorResponse.builder(e, HttpStatusCode.valueOf(404), e.getMessage())
                    .type(URI.create(e.getClass().getSimpleName())).build()));

    }

    private static Mono<Void> releasePartEvents(Flux<PartEvent> partEvents) {
        return partEvents.doOnNext(AttachmentEndpoint::releasePartEvent).then();
    }

    private static void releasePartEvent(PartEvent partEvent) {
        org.springframework.core.io.buffer.DataBufferUtils.release(partEvent.content());
    }

    /** 返回服务端权威媒体格式提示，不承担上传安全校验。 */
    private Mono<ServerResponse> listMediaFormats(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
            .bodyValue(MediaFilePolicy.formatHints());
    }

    private Mono<ServerResponse> getAttachmentTotal(ServerRequest request) {
        return Mono.empty();
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

        UUID parentId = UuidV7Utils.fromString(request.queryParam("parentId").orElse(null));

        Optional<String> typeOp = request.queryParam("type");
        AttachmentType type = typeOp.map(AttachmentType::valueOf).orElse(null);

        return Mono.just(AttachmentSearchCondition.builder()
                .page(page).size(size).type(type)
                .name(name).parentId(parentId)
                .build())
            .flatMap(attachmentService::listByCondition)
            .flatMap(pagingWrap -> ServerResponse.ok().bodyValue(pagingWrap));
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.findById(id)
            .flatMap(attachment -> ServerResponse.ok().bodyValue(attachment))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue("Not found for id: " + id));
    }

    private Mono<ServerResponse> getAttachmentPathDirsById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.findAttachmentPathDirsById(id)
            .flatMap(attachments -> ServerResponse.ok().bodyValue(attachments))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue("Not found for id: " + id));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.removeById(id)
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> createDirectory(ServerRequest request) {
        Optional<String> nameOp = request.queryParam("name");
        final String name = nameOp.isPresent() && StringUtils.hasText(nameOp.get())
            ? new String(Base64.getDecoder().decode(nameOp.get()), StandardCharsets.UTF_8)
            : "";

        UUID parentId = UUID.fromString(request.queryParam("parentId")
            .orElse(V_ROOT_DIRECTORY_PARENT_ID));

        return attachmentService.createDirectory(parentId, name)
            .flatMap(attachment -> ServerResponse.ok().bodyValue(attachment))
            .onErrorResume(AttachmentParentNotFoundException.class,
                e -> ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(e.getMessage()))
            .onErrorResume(DuplicateKeyException.class, e ->
                ServerResponse.status(HttpStatus.BAD_REQUEST)
                    .bodyValue("Duplicate directory for name: " + name));
    }

    private Mono<ServerResponse> listDirectory(ServerRequest request) {
        Optional<String> nameOp = request.queryParam("name");
        final String name = nameOp.isPresent() && StringUtils.hasText(nameOp.get())
            ? new String(Base64.getDecoder().decode(nameOp.get()), StandardCharsets.UTF_8)
            : "";

        UUID parentId = UUID.fromString(request.queryParam("parentId")
            .orElse(V_ROOT_DIRECTORY_PARENT_ID));

        return attachmentService.createDirectory(parentId, name)
            .flatMap(attachment -> ServerResponse.ok().bodyValue(attachment))
            .onErrorResume(AttachmentParentNotFoundException.class,
                e -> ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(e.getMessage()))
            .onErrorResume(DuplicateKeyException.class, e ->
                ServerResponse.status(HttpStatus.BAD_REQUEST)
                    .bodyValue("Duplicate directory for name: " + name));
    }

    private Mono<ServerResponse> update(ServerRequest request) {
        return request.bodyToMono(Attachment.class)
            .flatMap(attachmentService::save)
            .flatMap(attachment -> ServerResponse.ok().bodyValue(attachment))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue("Not found attachment record."));

    }

    private Mono<ServerResponse> generateFragmentUploadAttachmentUniqueId(ServerRequest request) {
        return Mono.justOrEmpty(UUID.randomUUID())
            .map(UUID::toString)
            .map(uuid -> uuid.replaceAll("-", ""))
            .flatMap(uuid -> ServerResponse.ok().bodyValue(uuid));
    }

    private Mono<ServerResponse> receiveFragmentUploadChunkAttachment(ServerRequest request) {
        List<String> uploadLengthList = request.headers().header("Upload-Length");
        Assert.notEmpty(uploadLengthList, "Request header 'Upload-Length' must has text.");

        final var uploadLength = Long.valueOf(uploadLengthList.get(0));
        List<String> uploadOffsetList = request.headers().header("Upload-Offset");
        Assert.notEmpty(uploadOffsetList, "Request header 'Upload-Offset' must has text.");

        final var uploadOffset = Long.valueOf(uploadOffsetList.get(0));
        List<String> uploadNameList = request.headers().header("Upload-Name");
        Assert.notEmpty(uploadNameList, "Request header 'Upload-Name' must has text.");

        final var uploadName = new String(Base64.getDecoder()
            .decode(uploadNameList.get(0).getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
        try {
            mediaValidationService.validateFilename(uploadName);
        } catch (IllegalArgumentException exception) {
            return ServerResponse.badRequest().bodyValue(exception.getMessage());
        }

        final String unique = request.pathVariable("unique");
        Assert.hasText(unique, "Request path var 'unique' must has text.");

        List<String> parentIdList = request.headers().header("PARENT-ID");
        UUID parentId =
            (parentIdList.isEmpty() || "undefined".equalsIgnoreCase(parentIdList.get(0)))
                ? null :
                UUID.fromString(parentIdList.get(0));

        Flux<DataBuffer> content = request.body(BodyExtractors.toDataBuffers())
            .doOnDiscard(DataBuffer.class,
                org.springframework.core.io.buffer.DataBufferUtils::release);
        return attachmentService.receiveAndHandleFragmentUploadChunkFile(
                unique, uploadLength, uploadOffset, uploadName, content, parentId)
            .then(ServerResponse.ok().bodyValue("SUCCESS"));
    }

    private Mono<ServerResponse> revertFragmentUploadAttachmentByUnique(ServerRequest request) {
        return request.bodyToMono(String.class)
            .flatMap(attachmentService::revertFragmentUploadFile)
            .then(ServerResponse.ok().bodyValue("SUCCESS"));
    }

    private Mono<ServerResponse> getDownloadUrl(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.getDownloadUrl(id)
            .flatMap(url -> ServerResponse.ok().bodyValue(url));
    }

    private Mono<ServerResponse> getReadUrl(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.getReadUrl(id)
            .flatMap(url -> ServerResponse.ok().bodyValue(url));
    }

    private Mono<ServerResponse> getUrlConditions(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.getUrlConditions(id)
            .flatMap(conditions -> ServerResponse.ok().bodyValue(conditions));
    }

    private Mono<ServerResponse> postUrlWithConditions(ServerRequest request) {
        return request.bodyToMono(UrlWithConditionsRequest.class)
            .flatMap(req -> attachmentService.getUrlWithConditions(
                req.attachmentId(), req.conditions()))
            .flatMap(url -> ServerResponse.ok().bodyValue(url));
    }

    private Mono<ServerResponse> getStreamById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.findById(id)
            .flatMap(attachment -> {
                String rangeHeader = request.headers().firstHeader(HttpHeaders.RANGE);
                if (rangeHeader != null) {
                    return doGetPartialContentRsp(id, attachment.getSize(),
                        attachment.getName(), rangeHeader);
                }
                return doGetFullContentRsp(id, attachment.getName());
            })
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(AttachmentUploadException.class,
                exception -> ServerResponse.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build());
    }

    private Mono<ServerResponse> getSvgPreviewById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return attachmentService.findById(id)
            .filter(this::isSvg)
            .flatMap(attachment -> attachmentService.getStreamById(id)
                .flatMap(stream -> {
                    if (!"image/svg+xml".equals(stream.getContextType())) {
                        return ServerResponse.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
                    }
                    return ServerResponse.ok()
                        .header(HttpHeaders.CONTENT_TYPE, stream.getContextType())
                        .header("Content-Security-Policy",
                            "sandbox; default-src 'none'; style-src 'unsafe-inline'; img-src data:")
                        .header("X-Content-Type-Options", "nosniff")
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                            buildContentDisposition(attachment.getName(), stream.getContextType()))
                        .body(stream.getDataBufferFlux(), DataBuffer.class);
                }))
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(AttachmentUploadException.class,
                exception -> ServerResponse.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build());
    }

    private boolean isSvg(Attachment attachment) {
        String name = attachment.getName();
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".svg");
    }

    private Mono<ServerResponse> doGetPartialContentRsp(
        UUID aid, Long fileSize, String fileName, String rangeHeader) {
        try {
            long[] range = parseRange(rangeHeader, fileSize);
            long start = range[0];
            long end = range[1];
            long contentLength = end - start + 1;
            return attachmentService.getStreamByIdWithRange(aid, start, end)
                .flatMap(stream -> ServerResponse.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, stream.getContextType())
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE,
                        String.format("bytes %d-%d/%d", start, end, fileSize))
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        buildContentDisposition(fileName, stream.getContextType()))
                    .body(stream.getDataBufferFlux(), DataBuffer.class));
        } catch (IllegalArgumentException exception) {
            return rangeNotSatisfiable(fileSize);
        }
    }

    private Mono<ServerResponse> doGetFullContentRsp(UUID aid, String fileName) {
        return attachmentService.getStreamById(aid)
            .flatMap(stream -> ServerResponse.ok()
                .header(HttpHeaders.CONTENT_TYPE, stream.getContextType())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(stream.getContextLength()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    buildContentDisposition(fileName, stream.getContextType()))
                .body(stream.getDataBufferFlux(), DataBuffer.class));
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        if (fileSize <= 0 || rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            throw new IllegalArgumentException("无效的范围请求");
        }
        String value = rangeHeader.substring(6).trim();
        if (value.isEmpty() || value.contains(",")) {
            throw new IllegalArgumentException("仅支持单一字节范围");
        }
        int separator = value.indexOf('-');
        if (separator < 0 || separator != value.lastIndexOf('-')) {
            throw new IllegalArgumentException("无效的字节范围格式");
        }
        String startValue = value.substring(0, separator).trim();
        String endValue = value.substring(separator + 1).trim();
        long start;
        long end;
        if (startValue.isEmpty()) {
            long suffixLength = Long.parseLong(endValue);
            if (suffixLength <= 0) {
                throw new IllegalArgumentException("无效的后缀范围");
            }
            start = Math.max(0, fileSize - suffixLength);
            end = fileSize - 1;
        } else {
            start = Long.parseLong(startValue);
            end = endValue.isEmpty() ? fileSize - 1 : Long.parseLong(endValue);
        }
        if (start < 0 || start >= fileSize || end < start || end >= fileSize) {
            throw new IllegalArgumentException("请求范围超出附件长度");
        }
        return new long[] {start, end};
    }

    private Mono<ServerResponse> rangeNotSatisfiable(Long fileSize) {
        return ServerResponse.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
            .build();
    }

    private String buildContentDisposition(String fileName, String contentType) {
        String safeFileName = StringUtils.hasText(fileName)
            ? fileName.replace('\r', '_').replace('\n', '_') : "attachment";
        ContentDisposition.Builder builder = isTextAttachment(contentType)
            ? ContentDisposition.attachment() : ContentDisposition.inline();
        return builder.filename(safeFileName, StandardCharsets.UTF_8).build().toString();
    }

    private boolean isTextAttachment(String contentType) {
        return contentType.startsWith("text/")
            || "application/x-subrip".equals(contentType)
            || "application/ttml+xml".equals(contentType)
            || "application/x-sami".equals(contentType)
            || "application/smil+xml".equals(contentType)
            || "application/xml".equals(contentType);
    }

    /**
     * 带条件的附件访问地址请求参数.
     */
    public record UrlWithConditionsRequest(
        @io.swagger.v3.oas.annotations.media.Schema(
            description = "附件ID") UUID attachmentId,
        @io.swagger.v3.oas.annotations.media.Schema(
            description = "条件参数，如 {\"quality\":\"4k\",\"vipToken\":\"xxx\"}")
        Map<String, Object> conditions
    ) {}
}
