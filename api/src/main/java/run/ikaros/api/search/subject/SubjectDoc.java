package run.ikaros.api.search.subject;

import lombok.Data;
import run.ikaros.api.store.enums.SubjectType;

@Data
public class SubjectDoc {
    private SubjectType type;
    private String name;
    private String nameCn;
    private String infobox;
    private String summary;
    private Boolean nsfw;
    private Long airTime;
}
