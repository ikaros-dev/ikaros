package run.ikaros.server.custom.router;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.CustomConverter;
import run.ikaros.server.custom.GroupVersionKind;
import run.ikaros.server.custom.ReactiveCustomClient;
import run.ikaros.server.infra.warp.PagingWrap;

public class CustomRouterFunctionFactory {

    private final ReactiveCustomClient client;
    private final Class<?> clazz;

    public CustomRouterFunctionFactory(ReactiveCustomClient client, Custom custom, Class<?> clazz) {
        this.client = client;
        this.clazz = clazz;
    }

    /**
     * custom RouterFunction.
     *
     * @return custom RouterFunction
     */
    @NonNull
    public RouterFunction<ServerResponse> create() {
        var createHandler = new CustomCreateHandler(client, clazz);
        var deleteHandler = new CustomDeleteHandler(client, clazz);
        var getHandler = new CustomGetHandler(client, clazz);
        var listHandler = new CustomListHandler(client, clazz);
        var updateHandler = new CustomUpdateHandler(client, clazz);
        GroupVersionKind gvk = CustomConverter.gvk(clazz);
        String kind = gvk.kind();
        return SpringdocRouteBuilder.route()
            .GET(getHandler.pathPattern(), getHandler,
                builder -> builder.operationId("Get" + gvk)
                    .description("Get " + gvk)
                    .tag(kind)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + kind))
                    .response(responseBuilder().responseCode("200")
                        .description("Response single " + kind)
                        .implementation(clazz)))
            .GET(listHandler.pathPattern(), listHandler,
                builder -> {
                    builder.operationId("List" + gvk)
                        .description("List " + gvk)
                        .tag(kind)
                        .response(responseBuilder().responseCode("200")
                            .description("Response " + kind)
                            .implementation(PagingWrap.class));
                })
            .POST(createHandler.pathPattern(), createHandler,
                builder -> builder.operationId("Create" + gvk)
                    .description("Create " + gvk)
                    .tag(kind)
                    .requestBody(requestBodyBuilder()
                        .description("Fresh " + kind)
                        .implementation(clazz))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + kind + " created just now")
                        .implementation(clazz)))
            .PUT(updateHandler.pathPattern(), updateHandler,
                builder -> builder.operationId("Update" + gvk)
                    .description("Update " + gvk)
                    .tag(kind)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + kind))
                    .requestBody(requestBodyBuilder()
                        .description("Updated " + kind)
                        .implementation(clazz))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + kind + " updated just now")
                        .implementation(clazz)))
            .DELETE(deleteHandler.pathPattern(), deleteHandler,
                builder -> builder.operationId("Delete" + gvk)
                    .description("Delete " + gvk)
                    .tag(kind)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + kind))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + kind + " deleted just now")))
            .build();
    }

    interface PathPatternGenerator {

        String pathPattern();

        static String buildExtensionPathPattern(Custom custom) {
            return "/apis"
                + '/' + custom.group()
                + '/' + custom.version()
                + '/' + custom.kind();
        }
    }

    interface GetHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface ListHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface CreateHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface UpdateHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface DeleteHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

}
