package run.ikaros.server.core.file;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.api.constant.OpenApiConst;
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


            .build();
    }
}
