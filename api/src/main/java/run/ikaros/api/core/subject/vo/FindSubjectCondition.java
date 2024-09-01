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
    /**
     * 数字年份，则直接匹配，如 2020
     * 年份范围，如 2012-2020
     * 年份范围大于小于，如 lt1970
     *  .
     */
    private String year;
    /**
     * 1, 4, 7, 10.
     */
    private Integer month;
    /**
     * default is false.
     */
    private Boolean airTimeDesc;
    /**
     * default is true.
     */
    private Boolean updateTimeDesc;
    /**
     * default is null.
     */
    private Boolean isEnd;

    /**
     * init default if field value is null.
     *
     * @see #page
     * @see #size
     * @see #nsfw
     * @see #airTimeDesc
     * @see #updateTimeDesc
     */
    public void initDefaultIfNull() {
        if (Objects.isNull(page)) {
            page = 1;
        }
        if (Objects.isNull(size)) {
            page = 10;
        }
        if (Objects.isNull(airTimeDesc)) {
            airTimeDesc = false;
        }
        if (Objects.isNull(updateTimeDesc)) {
            updateTimeDesc = true;
        }

    }
}
