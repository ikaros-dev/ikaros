package run.ikaros.server.custom.router;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static run.ikaros.api.infra.utils.StringUtils.upperCaseFirst;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.api.custom.GroupVersionKind;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.infra.model.PagingWrap;

public class CustomRouterFunctionFactory {

    private final ReactiveCustomClient client;
    private final CustomScheme scheme;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public CustomRouterFunctionFactory(CustomScheme scheme, ReactiveCustomClient client,
                                       ApplicationEventPublisher applicationEventPublisher) {
        this.client = client;
        this.scheme = scheme;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * custom RouterFunction.
     *
     * @return custom RouterFunction
     */
    @NonNull
    public RouterFunction<ServerResponse> create() {
        var createHandler = new CustomCreateHandler(client, scheme, applicationEventPublisher);
        var deleteHandler = new CustomDeleteHandler(client, scheme, applicationEventPublisher);
        var getHandler = new CustomGetHandler(client, scheme);
        var listHandler = new CustomListHandler(client, scheme);
        var updateHandler = new CustomUpdateHandler(client, scheme, applicationEventPublisher);
        var listPagingHandler = new CustomListPagingHandler(client, scheme);
        var getMetaHandler = new CustomGetMetaHandler(client, scheme);
        var updateMetaHandler = new CustomUpdateMetaHandler(client, scheme,
            applicationEventPublisher);
        GroupVersionKind gvk = scheme.groupVersionKind();
        String tag = gvk.group() + '/' + gvk.version() + '/' + gvk.kind().toLowerCase();
        return SpringdocRouteBuilder.route()
            .GET(getHandler.pathPattern(), getHandler,
                builder -> builder.operationId("Get" + upperCaseFirst(scheme.singular()))
                    .description("Get " + scheme.singular() + " By Name.")
                    .tag(tag)
                    .parameter(parameterBuilder().in(ParameterIn.PATH)
                        .name("name")
                        .description("Name of " + gvk.kind()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response single " + gvk.kind())
                        .implementation(scheme.type())))
            .GET(getMetaHandler.pathPattern(), getMetaHandler,
                builder -> builder.operationId("Get" + upperCaseFirst(scheme.singular()) + "Meta")
                    .description("Get " + scheme.singular() + " meta value by name and metaName.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name").in(ParameterIn.PATH).required(true)
                        .description("Name of " + gvk.kind())
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("metaName").in(ParameterIn.PATH).required(true)
                        .description("MetaName of " + gvk.kind())
                        .implementation(String.class))
                    .response(responseBuilder().responseCode("200")
                        .description("Response single " + gvk.kind() + " metadata value.")))
            .GET(listHandler.pathPattern(), listHandler,
                builder -> builder.operationId("List" + upperCaseFirst(scheme.plural()))
                    .description("List " + scheme.plural())
                    .tag(tag)
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + gvk.kind())
                        .implementation(scheme.type())))
            .GET(listPagingHandler.pathPattern(), listPagingHandler,
                builder -> builder.operationId("Get" + upperCaseFirst(scheme.plural())
                        + "ByPaging.")
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
                builder -> builder.operationId("Create" + upperCaseFirst(scheme.singular()))
                    .description("Create " + scheme.singular())
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .description("Fresh " + gvk.kind())
                        .implementation(scheme.type()))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + gvk.kind() + " created just now")
                        .implementation(scheme.type())))
            .PUT(updateHandler.pathPattern(), updateHandler,
                builder -> builder.operationId("Update" + upperCaseFirst(scheme.singular()))
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
            .PUT(updateMetaHandler.pathPattern(), updateMetaHandler,
                builder -> builder.operationId(
                        "Update" + upperCaseFirst(scheme.singular()) + "Meta")
                    .description("Update " + scheme.singular() + " metadata value. ")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name").in(ParameterIn.PATH).required(true)
                        .description("Name of " + scheme.singular()))
                    .parameter(parameterBuilder()
                        .name("metaName").in(ParameterIn.PATH).required(true)
                        .description("MetaName of " + scheme.singular())
                        .implementation(String.class))
                    .requestBody(requestBodyBuilder()
                        .implementation(byte[].class)
                        .required(true)
                        .description("Updated " + upperCaseFirst(scheme.singular())
                            + " Metadata value. current request body "
                            + "receive data type is byte[].class, "
                            + "If you specific data type is a String.class, "
                            + "must to add English double quotation marks.  "
                            + "correct is: \"new value\".  "
                            + "incorrect is: new value."))
                    .response(responseBuilder().responseCode("200")
                        .description("Response " + gvk.kind() + " metadata value "
                            + " updated just now")
                        .implementation(scheme.type())))
            .DELETE(deleteHandler.pathPattern(), deleteHandler,
                builder -> builder.operationId("Delete" + upperCaseFirst(scheme.singular()))
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

    interface GetMetaHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface ListHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface CreateHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface UpdateHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface UpdateMetaHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

    interface DeleteHandler extends HandlerFunction<ServerResponse>, PathPatternGenerator {

    }

}
