package run.ikaros.server.core.subject;


import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.SubjectType;

@Data
@Builder
public class FindSubjectCondition {
    private Integer page;
    private Integer size;
    private String name;
    private String nameCn;
    private Boolean nsfw;
    private SubjectType type;
}
