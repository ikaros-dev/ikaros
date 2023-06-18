package run.ikaros.api.core.subject;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.SubjectRelationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SubjectRelation {
    @Schema(requiredMode = REQUIRED)
    private Long subject;

    /**
     * Subject relation type.
     *
     * @see SubjectRelationType#getCode()
     */
    @Schema(requiredMode = REQUIRED)
    @JsonProperty("relation_type")
    private SubjectRelationType relationType;

    @JsonProperty("relation_subjects")
    @Schema(requiredMode = REQUIRED)
    private Set<Long> relationSubjects;
}
