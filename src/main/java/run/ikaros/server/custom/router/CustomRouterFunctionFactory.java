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
import run.ikaros.server.custom.GroupVersionKind;
import run.ikaros.server.custom.ReactiveCustomClient;
import run.ikaros.server.custom.scheme.CustomScheme;
import run.ikaros.server.infra.warp.PagingWrap;

public class CustomRouterFunctionFactory {

    private final ReactiveCustomClient client;
    private final CustomScheme scheme;

    public CustomRouterFunctionFactory(CustomScheme scheme, ReactiveCustomClient client) {
        this.client = client;
        this.scheme = scheme;
    }

    /**
     * custom RouterFunction.
     *
     * @return custom RouterFunction
     */
    @NonNull
    public RouterFunction<ServerResponse> create() {
        var createHandler = new CustomCreateHandler(client, scheme);
        var deleteHandler = new CustomDeleteHandler(client, scheme);
        var getHandler = new CustomGetHandler(client, scheme);
        var listHandler = new CustomListHandler(client, scheme);
        var updateHandler = new CustomUpdateHandler(client, scheme);
        var listPagingHandler = new CustomListPagingHandler(client, scheme);
        GroupVersionKind gvk = scheme.groupVersionKind();
        String kind = gvk.kind();
        return SpringdocRouteBuilder.route()
            .GET(getHandler.pathPattern(), getHandler,
                builder -> builder.operationId("Get" + scheme.singular())
                    .description("Get " + scheme.singular())
                    .tag(kind)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + kind))
                    .response(responseBuilder().responseCode("200")
                        .description("Response single " + kind)
                        .implementation(scheme.type())))
            .GET(listHandler.pathPattern(), listHandler,
                builder -> {
                    builder.operationId("List" + scheme.plural())
                        .description("List " + scheme.plural())
                        .tag(kind)
                        .response(responseBuilder().responseCode("200")
                            .description("Response " + kind)
                            .implementation(scheme.type()));
                })
            .GET(listPagingHandler.pathPattern(), listPagingHandler,
                builder -> builder.operationId("Get" + scheme.plural() + "by paging.")
                    .description("Get " + scheme.plural() + " by paging.")
                    .tag(kind)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("page")
                        .description("Page of " + kind))
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("size")
                        .description("Size of" + kind))
                    .response(responseBuilder().responseCode("200")
                        .description("Response" + kind)
                        .implementation(PagingWrap.class)))
            .POST(createHandler.pathPattern(), createHandler,
                builder -> builder.operationId("Create" + scheme.singular())
                    .description("Create " + scheme.singular())
                    .tag(kind)
                    .requestBody(requestBodyBuilder()
                        .description("Fresh " + kind)
                        .implementation(scheme.type()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + kind + " created just now")
                        .implementation(scheme.type())))
            .PUT(updateHandler.pathPattern(), updateHandler,
                builder -> builder.operationId("Update" + scheme.singular())
                    .description("Update " + scheme.singular())
                    .tag(kind)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + scheme.singular()))
                    .requestBody(requestBodyBuilder()
                        .description("Updated " + kind)
                        .implementation(scheme.type()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + kind + " updated just now")
                        .implementation(scheme.type())))
            .DELETE(deleteHandler.pathPattern(), deleteHandler,
                builder -> builder.operationId("Delete" + scheme.singular())
                    .description("Delete " + scheme.singular())
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

        static String buildCustomPathPatternPrefix(CustomScheme scheme) {
            GroupVersionKind gvk = scheme.groupVersionKind();
            return "/apis"
                + '/' + gvk.group()
                + '/' + gvk.version();
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
