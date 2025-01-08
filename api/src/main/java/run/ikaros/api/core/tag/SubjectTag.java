package run.ikaros.api.core.tag;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SubjectTag {
    private Long id;
    private Long subjectId;
    private String name;
    private Long userId;
    private LocalDateTime createTime;
    /**
     * 十六进制字符串颜色，与条目无关，与标签名一对一，与用户ID关联.
     *
     * @see Tag
     */
    private String color;
}
