package run.ikaros.api.core.subject.vo;


import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.SubjectType;

@Data
@Builder
public class FindSubjectCondition {
    /**
     * default is 1.
     */
    private Integer page;
    /**
     * default is 10.
     */
    private Integer size;
    private String name;
    private String nameCn;
    private Boolean nsfw;
    private SubjectType type;
    private Integer year;
    private Integer month;
    /**
     * default is true.
     */
    private Boolean airTimeDesc;

    /**
     * init default if field value is null.
     *
     * @see #page
     * @see #size
     * @see #nsfw
     * @see #airTimeDesc
     */
    public void initDefaultIfNull() {
        if (Objects.isNull(page)) {
            page = 1;
        }
        if (Objects.isNull(size)) {
            page = 10;
        }
        if (Objects.isNull(airTimeDesc)) {
            airTimeDesc = true;
        }

    }
}
