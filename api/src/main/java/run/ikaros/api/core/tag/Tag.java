package run.ikaros.api.core.tag;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.TagType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Tag {
    private Long id;
    private TagType type;
    private Long masterId;
    private String name;
    private Long userId;
    private LocalDateTime createTime;
    /**
     * 十六进制字符串颜色，与条目无关，与标签名一对一，与用户ID关联.
     */
    private String color;
}
