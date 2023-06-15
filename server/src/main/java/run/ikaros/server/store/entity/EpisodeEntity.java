package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.entity.BaseEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "episode")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class EpisodeEntity extends BaseEntity {
    @Column("subject_id")
    private Long subjectId;
    private String name;
    @Column("name_cn")
    private String nameCn;
    private String description;
    @Column("air_time")
    private LocalDateTime airTime;
    private Integer sequence;
}
