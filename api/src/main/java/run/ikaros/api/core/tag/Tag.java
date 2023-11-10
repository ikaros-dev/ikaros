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
}
