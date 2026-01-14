package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.TagType;

/**
 * 标签，用户可以在同一类型的标签里共享，比如条目类型的标签可以多个条目共享.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "tag")
public class TagEntity {
    @Id
    private Long id;
    private TagType type;
    @Column("master_id")
    private Long masterId;
    private String name;
    @Column("user_id")
    private Long userId;
    @Column("create_time")
    private LocalDateTime createTime;
    /**
     * 十六进制字符串颜色，与条目无关，与标签名一对一，与用户ID关联.
     */
    private String color;
    private UUID uuid;
}
