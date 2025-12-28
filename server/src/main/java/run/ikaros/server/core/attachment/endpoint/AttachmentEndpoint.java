package run.ikaros.server.core.attachment.endpoint;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.web.reactive.function.BodyExtractors.toMultipartData;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.core.attachment.exception.AttachmentParentNotFoundException;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.utils.DataBufferUtils;

@Slf4j
@Component
public class AttachmentEndpoint implements CoreEndpoint {
    private final AttachmentService attachmentService;

    public AttachmentEndpoint(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
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
                            .schema(schemaBuilder().implementation(
                                DefaultUploadRequest.class))
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

            .GET("/attachment/{id}", this::getById,
                builder -> builder.operationId("GetAttachmentById").tag(tag)
                    .parameter(parameterBuilder().name("id")
                        .description("Attachment ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder().implementation(Attachment.class)))

            .GET("/attachment/paths/{id}", this::getAttachmentPathDirsById,
                builder -> builder.operationId("GetAttachmentPathDirsById")
                    .tag(tag).description("Get attachment path dirs by id.")
                    .parameter(parameterBuilder()
                        .name("id").description("Attachment id.")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder().implementationArray(Attachment.class)))

            .DELETE("/attachment/{id}", this::deleteById,
                builder -> builder.operationId("DeleteAttachment").tag(tag)
                    .parameter(parameterBuilder().name("id")
                        .description("Attachment ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class)))

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
                        .implementation(Long.class))
                    .response(responseBuilder().implementation(String.class)))

            .GET("/attachment/url/read/id/{id}", this::getReadUrl,
                builder -> builder.operationId("GetReadUrl")
                    .tag(tag).description("Get read url.")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("id")
                        .description("Read url.")
                        .implementation(Long.class))
                    .response(responseBuilder().implementation(String.class)))

            .GET("/attachment/stream/id/{id}", this::getStreamById,
                builder -> builder.operationId("GetStreamById")
                    .tag(tag).description("Get attachment stream by id.")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("id")
                        .description("Attachment id.")
                        .implementation(Long.class))
                    .response(responseBuilder().implementation(String.class)))

            .build();
    }

    public interface UploadRequest {

        @Schema(requiredMode = REQUIRED, description = "File")
        FilePart getFile();

    }

    public record DefaultUploadRequest(@Schema(hidden = true) MultiValueMap<String, Part> formData)
        implements UploadRequest {

        @Override
        public FilePart getFile() {
            if (formData.getFirst("file") instanceof FilePart file) {
                return file;
            }
            throw new ServerWebInputException("Invalid part of file");
        }

    }

    private Mono<ServerResponse> upload(ServerRequest request) {
        return request.body(toMultipartData())
            .map(DefaultUploadRequest::new)
            // Upload file by service.
            .flatMap(uploadRequest -> attachmentService.upload(
                AttachmentUploadCondition.builder()
                    .name(uploadRequest.getFile().filename())
                    .dataBufferFlux(DataBufferUtils.formFilePart(uploadRequest.getFile()))
                    .build()
            ))
            // Response upload file data
            .flatMap(file -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(file))
            .onErrorResume(NotFoundException.class, e -> ServerResponse.from(
                ErrorResponse.builder(e, HttpStatusCode.valueOf(404), e.getMessage())
                    .type(URI.create(e.getClass().getSimpleName())).build()));

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

        Optional<String> parentIdOp = request.queryParam("parentId");
        Long parentId = parentIdOp.isPresent() ? Long.parseLong(parentIdOp.get()) : null;

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
        String id = request.pathVariable("id");
        return attachmentService.findById(Long.parseLong(id))
            .flatMap(attachment -> ServerResponse.ok().bodyValue(attachment))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue("Not found for id: " + id));
    }

    private Mono<ServerResponse> getAttachmentPathDirsById(ServerRequest request) {
        String id = request.pathVariable("id");
        return attachmentService.findAttachmentPathDirsById(Long.parseLong(id))
            .flatMap(attachments -> ServerResponse.ok().bodyValue(attachments))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue("Not found for id: " + id));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        return attachmentService.removeById(Long.parseLong(id))
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> createDirectory(ServerRequest request) {
        Optional<String> nameOp = request.queryParam("name");
        final String name = nameOp.isPresent() && StringUtils.hasText(nameOp.get())
            ? new String(Base64.getDecoder().decode(nameOp.get()), StandardCharsets.UTF_8)
            : "";

        Optional<String> parentIdOp = request.queryParam("parentId");
        Long parentId = parentIdOp.isPresent() ? Long.parseLong(parentIdOp.get()) : null;

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

        Optional<String> parentIdOp = request.queryParam("parentId");
        Long parentId = parentIdOp.isPresent() ? Long.parseLong(parentIdOp.get()) : null;

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

        final String unique = request.pathVariable("unique");
        Assert.hasText(unique, "Request path var 'unique' must has text.");

        List<String> parentIdList = request.headers().header("PARENT-ID");
        Long parentId =
            (parentIdList.isEmpty() || "undefined".equalsIgnoreCase(parentIdList.get(0)))
                ? null :
                Long.parseLong(parentIdList.get(0));

        return request.body(BodyExtractors.toDataBuffers())
            .publishOn(Schedulers.boundedElastic())
            .<byte[]>handle((dataBuffer, sink) -> {
                try (InputStream inputStream = dataBuffer.asInputStream(true)) {
                    sink.next(inputStream.readAllBytes());
                } catch (IOException e) {
                    sink.error(new RuntimeException(e));
                }
            })
            .reduce((bytes, bytes2) -> {
                byte[] result = new byte[bytes.length + bytes2.length];
                System.arraycopy(bytes, 0, result, 0, bytes.length);
                System.arraycopy(bytes2, 0, result, bytes.length, bytes2.length);
                return result;
            })
            .flatMap(bytes -> attachmentService.receiveAndHandleFragmentUploadChunkFile(
                unique, uploadLength, uploadOffset, uploadName, bytes, parentId
            ))
            .then(ServerResponse.ok().bodyValue("SUCCESS"));
    }

    private Mono<ServerResponse> revertFragmentUploadAttachmentByUnique(ServerRequest request) {
        return request.bodyToMono(String.class)
            .flatMap(attachmentService::revertFragmentUploadFile)
            .then(ServerResponse.ok().bodyValue("SUCCESS"));
    }

    private Mono<ServerResponse> getDownloadUrl(ServerRequest request) {
        String id = request.pathVariable("id");
        return attachmentService.getDownloadUrl(Long.parseLong(id))
            .flatMap(url -> ServerResponse.ok().bodyValue(url));
    }

    private Mono<ServerResponse> getReadUrl(ServerRequest request) {
        String id = request.pathVariable("id");
        return attachmentService.getReadUrl(Long.parseLong(id))
            .flatMap(url -> ServerResponse.ok().bodyValue(url));
    }

    private Mono<ServerResponse> getStreamById(ServerRequest request) {
        final String id = request.pathVariable("id");
        return Mono.fromCallable(() -> {
            Mono<Attachment> attMono =
                attachmentService.findById(Long.valueOf(id));
            String rangeHeader = request.headers().firstHeader(HttpHeaders.RANGE);
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return attMono
                    .flatMap(att -> doGetPartialContentRsp(Long.valueOf(id), att.getSize(),
                        att.getName(), rangeHeader));
                // return handlePartialContent(filePath, rangeHeader, fileSize);
            }
            return attMono.flatMap(att ->
                doGetFullContentRsp(Long.valueOf(id), att.getSize(), att.getName()));
            // return handleFullContent(filePath, fileSize);
        }).flatMap(response -> response);
    }

    private Mono<ServerResponse> doGetPartialContentRsp(
        Long aid, Long fileSize, String fileName, String rangeHeader) {
        try {
            String range = rangeHeader.substring(6);
            String[] ranges = range.split("-");

            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileSize - 1;

            // 确保范围有效
            if (start < 0 || end >= fileSize || start > end) {
                return ServerResponse.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
            }

            long contentLength = end - start + 1;
            String contentType = buildContentType(FileUtils.parseFilePostfix(fileName));

            return attachmentService.getStreamByIdWithRange(aid, start, end)
                .flatMap(body -> ServerResponse.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE,
                        String.format("bytes %d-%d/%d", start, end, fileSize))
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .body(body, DataBuffer.class));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue("处理范围请求失败: " + e.getMessage());
        }
    }

    private Mono<ServerResponse> doGetFullContentRsp(Long aid, Long fileSize, String fileName) {
        String contentType = buildContentType(FileUtils.parseFilePostfix(fileName));
        return attachmentService.getStreamByIdWithoutRange(aid)
            .flatMap(body -> ServerResponse.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(body, DataBuffer.class));
    }

    private String buildContentType(String postfix) {
        String contentType = "";
        if (FileUtils.isDocument(postfix)) {
            contentType = "text/plain; charset=utf-8";
        } else if (FileUtils.isImage(postfix)) {
            contentType = "image/" + postfix;
        } else if (FileUtils.isVoice(postfix)) {
            contentType = "audio/mpeg";
        } else {
            contentType = "video/mp4";
        }
        return contentType;
    }

}
