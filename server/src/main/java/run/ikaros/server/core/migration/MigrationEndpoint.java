package run.ikaros.server.core.migration;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.endpoint.CoreEndpoint;

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
}
