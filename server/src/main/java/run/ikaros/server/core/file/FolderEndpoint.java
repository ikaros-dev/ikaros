package run.ikaros.server.core.file;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.file.Folder;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class FolderEndpoint implements CoreEndpoint {
    private final FolderService folderService;

    public FolderEndpoint(FolderService folderService) {
        this.folderService = folderService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/Folder";
        return SpringdocRouteBuilder.route()
            .POST("/folder", this::createFolder,
                builder -> builder.operationId("CreateFolder")
                    .tag(tag)
                    .description("Create folder")
                    .parameter(parameterBuilder()
                        .name("parentId").description("Parent folder id.")
                        .required(false).implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("name").required(true)
                        .description("Folder name, encode by base64.")
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementation(Folder.class)))

            .DELETE("/folder", this::deleteFolder,
                builder -> builder.operationId("DeleteFolder")
                    .tag(tag)
                    .description("Delete folder")
                    .parameter(parameterBuilder()
                        .name("id").required(true)
                        .description("Folder id.")
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("allowDeleteWhenChildExists")
                        .description("Allow delete when children exists.")
                        .implementation(Boolean.class)))

            .PUT("/folder/name", this::updateFolderName,
                builder -> builder.operationId("UpdateFolderName")
                    .tag(tag).description("Update folder name by id and new name.")
                    .parameter(parameterBuilder()
                        .name("id").description("Folder id")
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("newName").description("Folder new name.")
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementation(Folder.class)))

            .PUT("/folder/parent", this::moveFolder,
                builder -> builder.operationId("MoveFolder")
                    .tag(tag).description("Move folder, update parent id.")
                    .parameter(parameterBuilder()
                        .name("id").description("Folder id")
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("newParentId").description("Parent folder id")
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(Folder.class)))

            .GET("/folder/{id}", this::findById,
                builder -> builder.operationId("FindById")
                    .tag(tag).description("Find folder by id.")
                    .parameter(parameterBuilder()
                        .name("id").description("Folder id")
                        .in(ParameterIn.PATH)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(Folder.class)))

            .GET("/folder/name", this::findByParentIdAndName,
                builder -> builder.operationId("FindByParentIdAndName")
                    .tag(tag).description("Find by parent id and name")
                    .parameter(parameterBuilder()
                        .name("parentId").description("Parent folder id")
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("name").description("Folder name, encode by base64.")
                        .implementation(String.class))
                    .response(responseBuilder().implementation(Folder.class)))

            .GET("/folder/name/like", this::findByParentIdAndNameLike,
                builder -> builder.operationId("FindByParentIdAndNameLike")
                    .tag(tag).description("Find by parent id and name like")
                    .parameter(parameterBuilder()
                        .name("parentId").description("Parent folder id")
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("name").description("Folder name keyword, encode by base64.")
                        .implementation(String.class))
                    .response(responseBuilder().implementation(Folder.class)))

            .build();
    }

    Mono<ServerResponse> findByParentIdAndNameLike(ServerRequest request) {
        Long parentId = Long.valueOf(request.queryParam("parentId").orElse("-1"));
        String name = request.queryParam("name").orElse("");
        return folderService.findByParentIdAndNameLike(parentId, name)
            .collectList()
            .flatMap(folders -> ServerResponse.ok().bodyValue(folders));
    }

    Mono<ServerResponse> findByParentIdAndName(ServerRequest request) {
        Long parentId = Long.valueOf(request.queryParam("parentId").orElse("-1"));
        String name = request.queryParam("name").orElse("");
        return folderService.findByParentIdAndName(parentId, name)
            .flatMap(folder -> ServerResponse.ok().bodyValue(folder));
    }

    Mono<ServerResponse> findById(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return folderService.findById(id)
            .flatMap(folder -> ServerResponse.ok().bodyValue(folder));
    }


    Mono<ServerResponse> moveFolder(ServerRequest request) {
        Long id = Long.valueOf(request.queryParam("id").orElse("-1"));
        Long newParentId = Long.valueOf(request.queryParam("newParentId").orElse("-1"));
        return folderService.move(id, newParentId)
            .flatMap(folder -> ServerResponse.ok().bodyValue(folder));
    }


    Mono<ServerResponse> updateFolderName(ServerRequest request) {
        Long id = Long.valueOf(request.queryParam("id").orElse("-1"));
        String newName = request.queryParam("newName").orElse("");
        return folderService.updateName(id, newName)
            .flatMap(folder -> ServerResponse.ok().bodyValue(folder));
    }


    Mono<ServerResponse> deleteFolder(ServerRequest request) {
        Long id = Long.valueOf(request.queryParam("id").orElse("-1"));
        boolean allow =
            Boolean.parseBoolean(request.queryParam("allowDeleteWhenChildExists").orElse("-1"));
        return folderService.delete(id, allow)
            .then(ServerResponse.ok().build());
    }

    Mono<ServerResponse> createFolder(ServerRequest request) {
        Optional<String> parentIdOp = request.queryParam("parentId");
        Optional<String> nameOp = request.queryParam("name");
        Assert.isTrue(nameOp.isEmpty() || StringUtils.isBlank(nameOp.get()),
            "'name' must has text");

        final Long parentId =
            Long.valueOf(parentIdOp.orElse(String.valueOf(FileConst.DEFAULT_FOLDER_ID)));
        final String name = nameOp.orElse("");

        return request.bodyToMono(Folder.class)
            .flatMap(folder -> folderService.create(parentId, name))
            .flatMap(folder -> ServerResponse.ok().bodyValue(folder));
    }
}
