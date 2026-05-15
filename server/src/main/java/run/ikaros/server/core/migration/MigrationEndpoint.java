package run.ikaros.server.core.migration;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.web.reactive.function.BodyExtractors.toMultipartData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.attachment.endpoint.AttachmentEndpoint;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.utils.DataBufferUtils;

@Slf4j
@Component
public class MigrationEndpoint implements CoreEndpoint {
    private final MigrationService service;

    public MigrationEndpoint(MigrationService service) {
        this.service = service;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/migration";
        return SpringdocRouteBuilder.route()
            .GET("/migration/export/databse/table", this::exportDatabaseTable,
                builder -> builder.operationId("ExportDatabaseTable")
                    .tag(tag).description("ExportDatabaseTable")
                    .parameter(Builder.parameterBuilder()
                        .name("name")
                        .description("Table name.")
                        .example("subject"))
                    .response(responseBuilder()
                        .implementationArray(DataBuffer.class))
            )
            .GET("/migration/export/databse/tables", this::exportDatabaseTables,
                builder -> builder.operationId("ExportDatabaseTables")
                    .tag(tag).description("ExportDatabaseTables")
                    .response(responseBuilder()
                        .implementationArray(DataBuffer.class))
            )
            .POST("/migration/import/database/tables/csv/410x", this::importDatabaseTablesCsv410x,
                builder -> builder.operationId("ImportDatabaseTables4csv")
                    .tag(tag).description("从v1.0.x导出的CSV导入表数据")
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().implementation(DefaultUploadRequest.class))
                        ))
                    .response(responseBuilder()
                        .implementation(String.class)))


            .build();
    }

    private Mono<ServerResponse> exportDatabaseTable(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        Flux<DefaultDataBuffer> csvStream = service.exportDatabaseTable(name);
        return ServerResponse.ok()
            .header("Content-Type", "text/csv; charset=UTF-8")
            .header("Content-Disposition",
                "attachment; filename=\"" + name + ".csv\"")
            .header("Transfer-Encoding", "chunked")
            .body(csvStream, DataBuffer.class);
    }


    private Mono<ServerResponse> exportDatabaseTables(ServerRequest request) {
        Flux<DataBuffer> csvStream = service.exportDatabaseTables();
        String name = UuidV7Utils.generate();
        return ServerResponse.ok()
            .header("Content-Type", "application/zip; charset=UTF-8")
            .header("Content-Disposition",
                "attachment; filename=\"" + name + ".zip\"")
            .header("Transfer-Encoding", "chunked")
            .body(csvStream, DataBuffer.class);
    }

    private Mono<ServerResponse> importDatabaseTablesCsv410x(ServerRequest request) {
        return request.body(toMultipartData())
            .map(DefaultUploadRequest::new)
            .map(DefaultUploadRequest::getFile)
            .map(DataBufferUtils::formFilePart)
            .flatMap(service::importDatabaseTablesCsv410x)
            .flatMap(res -> ServerResponse.ok().bodyValue(res));
    }

    public record DefaultUploadRequest(@Schema(hidden = true) MultiValueMap<String, Part> formData)
        implements AttachmentEndpoint.UploadRequest {

        @Override
        public FilePart getFile() {
            if (formData.getFirst("file") instanceof FilePart file) {
                return file;
            }
            throw new ServerWebInputException("Invalid part of file");
        }

    }
}


