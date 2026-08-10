package run.ikaros.api.core.subject.vo;


import java.util.Objects;
import java.util.Set;
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
    /**
     * 同时匹配原名和中文名的关键词。
     */
    private String keyword;
    private Boolean nsfw;
    private SubjectType type;
    /**
     * 需要匹配的条目类型集合。
     */
    @Builder.Default
    private Set<SubjectType> types = Set.of();
    private String time;
    /**
     * default is true.
     */
    private Boolean airTimeDesc;
    /**
     * default is false.
     */
    private Boolean updateTimeDesc;
    /**
     * default is null.
     */
    private Boolean scoreDesc;

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
            size = 10;
        }
        if (Objects.isNull(types)) {
            types = Set.of();
        }
        if (Objects.isNull(airTimeDesc)) {
            airTimeDesc = true;
        }

    }
}
