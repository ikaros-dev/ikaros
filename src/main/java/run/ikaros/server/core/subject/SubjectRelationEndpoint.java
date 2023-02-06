// package run.ikaros.server.core.subject;
//
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;
// import org.springframework.web.reactive.function.server.RouterFunction;
// import org.springframework.web.reactive.function.server.ServerResponse;
// import run.ikaros.server.endpoint.CoreEndpoint;
// import run.ikaros.server.infra.constant.OpenApiConst;
//
// @Slf4j
// @Component
// public class SubjectRelationEndpoint implements CoreEndpoint {
//     @Override
//     public RouterFunction<ServerResponse> endpoint() {
//         var tag = OpenApiConst.CORE_VERSION + "/SubjectRelation";
//         return SpringdocRouteBuilder.route()
//             .build();
//     }
// }
