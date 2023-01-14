package run.ikaros.server.core.file;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.constant.OpenApiConst;
import run.ikaros.server.plugin.ExtensionComponentsFinder;

@Slf4j
@Component
public class FileEndpoint implements CoreEndpoint {

    private final ExtensionComponentsFinder extensionComponentsFinder;

    public FileEndpoint(ExtensionComponentsFinder extensionComponentsFinder) {
        this.extensionComponentsFinder = extensionComponentsFinder;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_GROUP + "/File";
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
            .GET("/files", this::search,
                builder -> {
                    builder
                        .operationId("SearchFiles")
                        .tag(tag);
                }
            )
            .DELETE("/files", this::delete,
                builder -> builder.operationId("DeleteFiles").tag(tag))
            .build();
    }


    Mono<ServerResponse> upload(ServerRequest request) {
        List<FileHandler> extensions = extensionComponentsFinder.getExtensions(FileHandler.class);
        return Mono.empty();
    }

    Mono<ServerResponse> search(ServerRequest request) {
        return Mono.empty();
    }

    Mono<ServerResponse> delete(ServerRequest request) {
        return Mono.empty();
    }

    public interface UploadRequest {

        @Schema(requiredMode = REQUIRED, description = "File")
        FilePart getFile();

        @Schema(requiredMode = REQUIRED, description = "Storage policy name")
        String getPolicyName();

    }
}
