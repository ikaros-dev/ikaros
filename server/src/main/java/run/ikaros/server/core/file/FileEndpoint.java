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
import java.net.URI;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.FileHandler;
import run.ikaros.api.core.file.FilePolicy;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Component
public class FileEndpoint implements CoreEndpoint {

    private final ExtensionComponentsFinder extensionComponentsFinder;
    private final ReactiveCustomClient reactiveCustomClient;
    private final FileRepository fileRepository;

    /**
     * File {@link CoreEndpoint} for file request.
     *
     * @param extensionComponentsFinder extension finder
     * @param reactiveCustomClient      custom client
     * @param fileRepository            file repository
     */
    public FileEndpoint(ExtensionComponentsFinder extensionComponentsFinder,
                        ReactiveCustomClient reactiveCustomClient, FileRepository fileRepository) {
        this.extensionComponentsFinder = extensionComponentsFinder;
        this.reactiveCustomClient = reactiveCustomClient;
        this.fileRepository = fileRepository;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/File";
        return SpringdocRouteBuilder.route()
            .POST("/files/upload", contentType(MediaType.MULTIPART_FORM_DATA), this::upload,
                builder -> builder
                    .operationId("UploadFile")
                    .tag(tag)
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().implementation(UploadRequest.class))
                        ))
                    .response(responseBuilder().implementation(File.class))
                    .build())
            .GET("/files", this::list,
                builder -> {
                    builder
                        .operationId("SearchFiles")
                        .tag(tag);
                }
            )
            .DELETE("/file/{id}", this::deleteById,
                builder -> builder.operationId("DeleteFile").tag(tag)
                    .parameter(parameterBuilder().name("id")
                        .description("File ID")
                        .in(ParameterIn.PATH)
                        .required(true).implementation(
                            Long.class)))
            // TODO large multipart file upload support
            .build();
    }


    Mono<ServerResponse> upload(ServerRequest request) {
        return request.body(toMultipartData())
            .map(DefaultUploadRequest::new)
            // Check request file policy exists.
            .flatMap(uploadRequest -> reactiveCustomClient.findOne(FilePolicy.class,
                    Objects.requireNonNull(uploadRequest.getPolicyName()).toUpperCase())
                .onErrorResume(NotFoundException.class, error -> Mono.error(new NotFoundException(
                    "Not found file policy: " + uploadRequest.getPolicyName())))
                .flatMap(filePolicy -> Mono.just(new FileHandler.DefaultUploadContext(
                    uploadRequest.getFile(), filePolicy.getName(), null))))
            .flatMap(uploadContext -> Flux.fromStream(
                    extensionComponentsFinder.getExtensions(FileHandler.class).stream())
                // Select file handler
                .filter(
                    fileHandler -> uploadContext.policy().equalsIgnoreCase(fileHandler.policy()))
                .switchIfEmpty(Mono.error(new NotFoundException(
                    "Not found file handler for policy: " + uploadContext.policy())))
                .collectList()
                .flatMap(fileHandlers -> Mono.just(fileHandlers.get(0)))
                // Do upload file
                .flatMap(fileHandler -> fileHandler.upload(uploadContext)))
            // Response upload file data
            .flatMap(file -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(file))
            .onErrorResume(NotFoundException.class, e -> ServerResponse.from(
                ErrorResponse.builder(e, HttpStatusCode.valueOf(404), e.getMessage())
                    .type(URI.create(e.getClass().getSimpleName())).build()));
    }

    Mono<ServerResponse> list(ServerRequest request) {
        return fileRepository.findAll()
            .map(File::new)
            .collectList()
            .flatMap(files -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(files));
    }

    Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        return Mono.just(id)
            .flatMap(fileId -> Mono.just(Long.valueOf(fileId)))
            .flatMap(fileRepository::findById)
            .map(File::new)
            .flatMap(file -> Flux.fromStream(
                    extensionComponentsFinder.getExtensions(FileHandler.class).stream())
                .filter(fileHandler -> fileHandler.policy()
                    .equalsIgnoreCase(file.entity().getPlace().toString()))
                .collectList().flatMap(fileHandlers -> Mono.just(fileHandlers.get(0)))
                .flatMap(fileHandler -> fileHandler.delete(file)))
            .flatMap(file -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("Delete success"))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("Not found for id: " + id));
    }

    public interface UploadRequest {

        @Schema(requiredMode = REQUIRED, description = "File")
        FilePart getFile();

        @Schema(requiredMode = REQUIRED, description = "Storage policy name")
        String getPolicyName();

    }

    public record DefaultUploadRequest(MultiValueMap<String, Part> formData)
        implements UploadRequest {

        @Override
        public FilePart getFile() {
            if (formData.getFirst("file") instanceof FilePart file) {
                return file;
            }
            throw new ServerWebInputException("Invalid part of file");
        }

        @Override
        public String getPolicyName() {
            if (formData.getFirst("policyName") instanceof FormFieldPart form) {
                return form.value();
            }
            throw new ServerWebInputException("Invalid part of policyName");
        }

    }
}
