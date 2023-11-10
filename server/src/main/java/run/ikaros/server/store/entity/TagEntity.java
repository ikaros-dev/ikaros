package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.TagType;

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
}
