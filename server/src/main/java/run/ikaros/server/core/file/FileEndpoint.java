package run.ikaros.server.core.file;

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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
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
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.file.request.FileBatchRequest;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.utils.DataBufferUtils;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.FileEntity;

@Slf4j
@Component
public class FileEndpoint implements CoreEndpoint {

    private final ExtensionComponentsFinder extensionComponentsFinder;
    private final ReactiveCustomClient reactiveCustomClient;
    private final FileService fileService;
    private final SubjectService subjectService;

    /**
     * File {@link CoreEndpoint} for file request.
     *
     * @param extensionComponentsFinder extension finder
     * @param reactiveCustomClient      custom client
     * @param fileService               file service
     * @param subjectService            subject service
     */
    public FileEndpoint(ExtensionComponentsFinder extensionComponentsFinder,
                        ReactiveCustomClient reactiveCustomClient,
                        FileService fileService, SubjectService subjectService) {
        this.extensionComponentsFinder = extensionComponentsFinder;
        this.reactiveCustomClient = reactiveCustomClient;
        this.fileService = fileService;
        this.subjectService = subjectService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/file";
        return SpringdocRouteBuilder.route()
            .POST("/files/upload", contentType(MediaType.MULTIPART_FORM_DATA), this::upload,
                builder -> builder
                    .operationId("UploadFile")
                    .tag(tag)
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().implementation(DefaultUploadRequest.class))
                        ))
                    .response(responseBuilder().implementation(File.class))
                    .build())
            .GET("/files", this::list,
                builder ->
                    builder
                        .operationId("ListFiles")
                        .tag(tag)
                        .response(responseBuilder().implementationArray(FileEntity.class)))

            .GET("/files/condition", this::listByCondition,
                builder -> builder.operationId("ListFilesByCondition")
                    .tag(tag).description("List files by condition.")
                    .parameter(parameterBuilder()
                        .name("page")
                        .description("第几页，从1开始, 默认为1.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("size")
                        .description("每页条数，默认为10.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("fileName")
                        .description("经过Basic64编码的文件名称，文件名称字段模糊查询。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("type")
                        .implementation(FileType.class))
                    .response(responseBuilder().implementation(PagingWrap.class))
            )

            .DELETE("/file/{id}", this::deleteById,
                builder -> builder.operationId("DeleteFile").tag(tag)
                    .parameter(parameterBuilder().name("id")
                        .description("File ID")
                        .in(ParameterIn.PATH)
                        .required(true).implementation(
                            Long.class)))

            .PUT("/file/update", this::update,
                builder -> builder.operationId("UpdateFile")
                    .tag(tag).description("Update file.")
                    .requestBody(Builder.requestBodyBuilder().implementation(FileEntity.class))
            )

            // Large multipart file fragment upload support
            .POST("/file/fragment/unique", this::generateFragmentUploadFileUniqueId,
                builder -> builder.operationId("GenerateFragmentUploadFileUniqueId")
                    .tag(tag).description("Generate fragment upload file unique id.")
                    .response(responseBuilder()
                        .description("Random uuid.")
                        .implementation(String.class)))
            .PATCH("/file/fragment/patch/{unique}", this::receiveFragmentUploadChunkFile,
                builder -> builder.operationId("ReceiveFragmentUploadChunkFile")
                    .tag(tag).description("Receive fragment upload chunk file.")
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("unique").required(true)
                        .description("Chunk file unique id."))
                    .parameter(parameterBuilder().in(ParameterIn.HEADER)
                        .name("Upload-Length").required(true)
                        .description("Upload chunk file length."))
                    .parameter(parameterBuilder().in(ParameterIn.HEADER)
                        .name("Upload-Offset").required(true)
                        .description("Upload chunk file offset."))
                    .parameter(parameterBuilder().in(ParameterIn.HEADER)
                        .name("Upload-Name").required(true)
                        .description("Upload chunk file file name.")))
            .DELETE("/file/fragment/revert", this::revertFragmentUploadFileByUnique,
                builder -> builder.operationId("RevertFragmentUploadFileByUniqueId")
                    .tag(tag).description("Revert fragment upload file by unique id.")
                    .requestBody(Builder.requestBodyBuilder()
                        .description("Unique id.")
                        .implementation(String.class)))

            .POST("/file/remote/push", this::pushFile,
                builder -> builder.operationId("PushFile2Remote")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("id")
                        .required(true)
                        .description("File id."))
                    .parameter(parameterBuilder()
                        .name("remote")
                        .required(true)
                        .description("Remote")))

            .POST("/file/remote/push/batch", this::pushFileBatch,
                builder -> builder.operationId("PushFile2RemoteBatch")
                    .tag(tag)
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(FileBatchRequest.class)))

            .POST("/file/remote/push/batch/subject/{subjectId}", this::pushSubject,
                builder -> builder.operationId("PushSubject2Remote")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .description("Subject id.")
                        .in(ParameterIn.PATH))
                    .parameter(parameterBuilder()
                        .name("remote")
                        .required(true)
                        .description("Remote")))

            .POST("/file/remote/pull", this::pullFile,
                builder -> builder.operationId("PullFile4Remote")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("id")
                        .required(true)
                        .description("File id."))
                    .parameter(parameterBuilder()
                        .name("remote")
                        .required(true)
                        .description("Remote")))

            .POST("/file/remote/pull/batch", this::pullFileBatch,
                builder -> builder.operationId("PullFile4RemoteBatch")
                    .tag(tag)
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(FileBatchRequest.class)))

            .POST("/file/remote/pull/batch/subject/{subjectId}", this::pullSubject,
                builder -> builder.operationId("PullSubject4Remote")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .description("Subject id.")
                        .in(ParameterIn.PATH))
                    .parameter(parameterBuilder()
                        .name("remote")
                        .required(true)
                        .description("Remote")))

            .PUT("/file/folder", this::moveFileFolder,
                builder -> builder.operationId("MoveFileToAppointFolder")
                    .tag(tag).description("Move file to appoint folder.")
                    .parameter(parameterBuilder()
                        .name("id").required(true)
                        .description("File id."))
                    .parameter(parameterBuilder()
                        .name("folderId").required(true)
                        .description("Folder id."))
                    .response(responseBuilder().implementation(File.class)))

            .build();
    }

    Mono<ServerResponse> moveFileFolder(ServerRequest request) {
        Long id = Long.valueOf(request.queryParam("id").orElse("-1"));
        Long folderId = Long.valueOf(request.queryParam("folderId").orElse("-1"));
        return fileService.updateFolder(id, folderId)
            .flatMap(file -> ServerResponse.ok().bodyValue(file))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> pushFile(ServerRequest request) {
        Optional<String> idOp = request.queryParam("id");
        Optional<String> remoteOp = request.queryParam("remote");
        return fileService.pushRemote(Long.valueOf(idOp.orElse("-1")), remoteOp.orElse(null))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> pushFileBatch(ServerRequest request) {
        return request.bodyToMono(FileBatchRequest.class)
            .flatMap(fileBatchRequest -> fileService.pushRemoteBatch(
                fileBatchRequest.getFileIds(), fileBatchRequest.getRemote()
            )).then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> pushSubject(ServerRequest request) {
        String remote = request.queryParam("remote").orElse("");
        String subjectId = request.pathVariable("subjectId");
        return subjectService.findById(Long.valueOf(subjectId))
            .map(Subject::getEpisodes)
            .map(episodes -> episodes.stream()
                .map(Episode::getResources)
                .filter(Objects::nonNull)
                .filter(resources -> !resources.isEmpty())
                .map(episodeResources -> episodeResources.get(0))
                .map(EpisodeResource::getAttachmentId)
                .toList())
            .flatMap(fileIds -> fileService.pushRemoteBatch(fileIds, remote))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> pullFile(ServerRequest request) {
        Optional<String> idOp = request.queryParam("id");
        Optional<String> remoteOp = request.queryParam("remote");
        return fileService.pullRemote(Long.valueOf(idOp.orElse("-1")), remoteOp.orElse(null))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> pullFileBatch(ServerRequest request) {
        return request.bodyToMono(FileBatchRequest.class)
            .flatMap(fileBatchRequest -> fileService.pullRemoteBatch(
                fileBatchRequest.getFileIds(), fileBatchRequest.getRemote()
            )).then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> pullSubject(ServerRequest request) {
        String remote = request.queryParam("remote").orElse("");
        String subjectId = request.pathVariable("subjectId");
        return subjectService.findById(Long.valueOf(subjectId))
            .map(Subject::getEpisodes)
            .map(episodes -> episodes.stream()
                .map(Episode::getResources)
                .filter(Objects::nonNull)
                .filter(resources -> !resources.isEmpty())
                .map(episodeResources -> episodeResources.get(0))
                .map(EpisodeResource::getAttachmentId)
                .toList())
            .flatMap(fileIds -> fileService.pullRemoteBatch(fileIds, remote))
            .then(ServerResponse.ok().build());
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

        Optional<String> fileNameOp = request.queryParam("fileName");
        final String fileName = fileNameOp.isPresent() && StringUtils.hasText(fileNameOp.get())
            ? new String(Base64.getDecoder().decode(fileNameOp.get()), StandardCharsets.UTF_8)
            : "";

        Optional<String> typeOp = request.queryParam("type");
        final FileType type = typeOp.isPresent() && StringUtils.hasText(typeOp.get())
            ? FileType.valueOf(typeOp.get())
            : null;

        return Mono.just(FindFileCondition.builder()
                .page(page).size(size).fileName(fileName)
                .type(type)
                .build())
            .flatMap(fileService::listEntitiesByCondition)
            .flatMap(pagingWrap -> ServerResponse.ok().bodyValue(pagingWrap));
    }

    private Mono<ServerResponse> update(ServerRequest request) {
        return request.bodyToMono(FileEntity.class)
            .flatMap(fileService::updateEntity)
            .then(ServerResponse.ok().bodyValue("SUCCESS"))
            .onErrorResume(NotFoundException.class, e -> ServerResponse
                .status(HttpStatus.NOT_FOUND).bodyValue("Not found file record."));
    }

    Mono<ServerResponse> generateFragmentUploadFileUniqueId(ServerRequest request) {
        return Mono.justOrEmpty(UUID.randomUUID())
            .map(UUID::toString)
            .map(uuid -> uuid.replaceAll("-", ""))
            .flatMap(uuid -> ServerResponse.ok().bodyValue(uuid));
    }

    Mono<ServerResponse> receiveFragmentUploadChunkFile(ServerRequest request) {
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
            .flatMap(bytes -> fileService.receiveAndHandleFragmentUploadChunkFile(
                unique, uploadLength, uploadOffset, uploadName, bytes
            ))
            .then(ServerResponse.ok().bodyValue("SUCCESS"));
    }

    Mono<ServerResponse> revertFragmentUploadFileByUnique(ServerRequest request) {
        return request.bodyToMono(String.class)
            .flatMap(fileService::revertFragmentUploadFile)
            .then(ServerResponse.ok().bodyValue("SUCCESS"));
    }

    Mono<ServerResponse> upload(ServerRequest request) {
        return request.body(toMultipartData())
            .map(DefaultUploadRequest::new)
            // Upload file by service.
            .flatMap(uploadRequest -> fileService.upload(
                uploadRequest.getFile().filename(),
                DataBufferUtils.formFilePart(uploadRequest.getFile())))
            // Response upload file data
            .flatMap(file -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(file))
            .onErrorResume(NotFoundException.class, e -> ServerResponse.from(
                ErrorResponse.builder(e, HttpStatusCode.valueOf(404), e.getMessage())
                    .type(URI.create(e.getClass().getSimpleName())).build()));
    }

    Mono<ServerResponse> list(ServerRequest request) {
        return fileService.findAll()
            .collectList()
            .flatMap(files -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(files));
    }

    Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        return fileService.deleteById(Long.parseLong(id))
            .then(ServerResponse.ok()
                .bodyValue("Delete success"))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue("Not found for id: " + id));
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
}
