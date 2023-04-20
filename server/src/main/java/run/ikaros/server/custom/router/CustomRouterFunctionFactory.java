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
import run.ikaros.api.custom.GroupVersionKind;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.custom.scheme.CustomScheme;

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
        String tag = gvk.group() + '/' + gvk.version() + '/' + gvk.kind();
        return SpringdocRouteBuilder.route()
            .GET(getHandler.pathPattern(), getHandler,
                builder -> builder.operationId("Get" + scheme.singular())
                    .description("Get " + scheme.singular())
                    .tag(tag)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + gvk.kind()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response single " + gvk.kind())
                        .implementation(scheme.type())))
            .GET(listHandler.pathPattern(), listHandler,
                builder -> {
                    builder.operationId("List" + scheme.plural())
                        .description("List " + scheme.plural())
                        .tag(tag)
                        .response(responseBuilder().responseCode("200")
                            .description("Response " + gvk.kind())
                            .implementation(scheme.type()));
                })
            .GET(listPagingHandler.pathPattern(), listPagingHandler,
                builder -> builder.operationId("Get" + scheme.plural() + "by paging.")
                    .description("Get " + scheme.plural() + " by paging.")
                    .tag(tag)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("page")
                        .description("Page of " + gvk.kind()))
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("size")
                        .description("Size of" + gvk.kind()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response" + gvk.kind())
                        .implementation(PagingWrap.class)))
            .POST(createHandler.pathPattern(), createHandler,
                builder -> builder.operationId("Create" + scheme.singular())
                    .description("Create " + scheme.singular())
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .description("Fresh " + gvk.kind())
                        .implementation(scheme.type()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + gvk.kind() + " created just now")
                        .implementation(scheme.type())))
            .PUT(updateHandler.pathPattern(), updateHandler,
                builder -> builder.operationId("Update" + scheme.singular())
                    .description("Update " + scheme.singular())
                    .tag(tag)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + scheme.singular()))
                    .requestBody(requestBodyBuilder()
                        .description("Updated " + gvk.kind())
                        .implementation(scheme.type()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + gvk.kind() + " updated just now")
                        .implementation(scheme.type())))
            .DELETE(deleteHandler.pathPattern(), deleteHandler,
                builder -> builder.operationId("Delete" + scheme.singular())
                    .description("Delete " + scheme.singular())
                    .tag(tag)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + gvk.kind()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + gvk.kind() + " deleted just now")))
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
