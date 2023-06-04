package run.ikaros.server.core.subject;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.utils.JsonUtils;

@Slf4j
@Component
public class SubjectRelationEndpoint implements CoreEndpoint {
    private final SubjectRelationService subjectRelationService;

    public SubjectRelationEndpoint(SubjectRelationService subjectRelationService) {
        this.subjectRelationService = subjectRelationService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/SubjectRelation";
        return SpringdocRouteBuilder.route()
            .GET("/subject-relations/{subjectId}", this::findAllBySubjectId,
                builder -> builder
                    .tag(tag)
                    .operationId("GetSubjectRelationsById")
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .in(ParameterIn.PATH)
                        .description("Subject id")
                        .implementation(Long.class)
                        .required(true)))
            .GET("/subject-relation/{subjectId}/{relationType}", this::findBySubjectIdAndType,
                builder -> builder
                    .tag(tag)
                    .operationId("GetSubjectRelationByIdAndType")
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .in(ParameterIn.PATH)
                        .description("Subject id")
                        .implementation(Long.class)
                        .required(true))
                    .parameter(parameterBuilder()
                        .name("relationType")
                        .in(ParameterIn.PATH)
                        .description("Subject relation type")
                        .implementation(SubjectRelationType.class)
                        .required(true)))
            .POST("/subject-relation", this::createSubjectRelation,
                builder -> builder
                    .tag(tag)
                    .operationId("CreateSubjectRelation")
                    .description("Create subject relation")
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .description("SubjectRelation")
                        .implementation(SubjectRelation.class)))
            .DELETE("/subject-relation", this::removeSubjectRelation,
                builder -> builder
                    .tag(tag)
                    .operationId("RemoveSubjectRelation")
                    .description("Remove subject relation")
                    .parameter(parameterBuilder()
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .name("subject_id")
                        .description("Subject id")
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .name("relation_type")
                        .description("Subject relation type code")
                        .implementation(SubjectRelationType.class))
                    .parameter(parameterBuilder()
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .name("relation_subjects")
                        .description("Relation subjects")
                        .implementation(String.class))
            ).build();
    }

    private Mono<ServerResponse> findAllBySubjectId(ServerRequest request) {
        return Mono.just(request.pathVariable("subjectId"))
            .map(Long::valueOf)
            .flatMap(subjectId -> subjectRelationService.findAllBySubjectId(subjectId)
                .collectList())
            .filter(subjectRelations -> !subjectRelations.isEmpty())
            .flatMap(subjectRelations -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subjectRelations))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> findBySubjectIdAndType(ServerRequest request) {
        String subjectId = request.pathVariable("subjectId");
        String relationType = request.pathVariable("relationType");
        return subjectRelationService.findBySubjectIdAndType(Long.valueOf(subjectId),
                StringUtils.isNumeric(relationType)
                    ? SubjectRelationType.codeOf(Integer.valueOf(relationType))
                    : SubjectRelationType.valueOf(relationType))
            .filter(subjectRelation -> !subjectRelation.getRelationSubjects().isEmpty())
            .flatMap(subjectRelation -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subjectRelation))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> createSubjectRelation(ServerRequest request) {
        return request.bodyToMono(SubjectRelation.class)
            .flatMap(subjectRelationService::createSubjectRelation)
            .filter(subjectRelation -> !subjectRelation.getRelationSubjects().isEmpty())
            .flatMap(subjectRelation -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subjectRelation))
            .switchIfEmpty(ServerResponse.notFound().build());
    }


    private Mono<ServerResponse> removeSubjectRelation(ServerRequest request) {
        Optional<String> subjectId = request.queryParam("subject_id");
        Assert.isTrue(subjectId.isPresent(), "'subject_id' must not be empty");
        Optional<String> relationType = request.queryParam("relation_type");
        Assert.isTrue(relationType.isPresent(), "'relation_type' must not be empty");
        Optional<String> relationSubjects = request.queryParam("relation_subjects");
        Assert.isTrue(relationSubjects.isPresent(), "'relation_subjects' must not be empty");
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(Long.valueOf(subjectId.get()))
            .relationType(
                StringUtils.isNumeric(relationType.get())
                    ? SubjectRelationType.codeOf(Integer.valueOf(relationType.get()))
                    : SubjectRelationType.valueOf(relationType.get()))
            .build();
        if (relationSubjects.get().startsWith("[") && relationSubjects.get().endsWith("]")) {
            subjectRelation.setRelationSubjects(
                Set.of(Objects.requireNonNull(
                    JsonUtils.json2ObjArr(relationSubjects.get(), new TypeReference<>() {
                    }))));
        } else {
            subjectRelation.setRelationSubjects(Set.of(Long.valueOf(relationSubjects.get())));
        }

        return Mono.just(subjectRelation)
            .flatMap(subjectRelationService::removeSubjectRelation)
            .flatMap(subjectRelation1 -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subjectRelation1));
    }
}
