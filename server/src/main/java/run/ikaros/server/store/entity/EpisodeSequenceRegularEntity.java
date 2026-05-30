package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.EpisodeGroup;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "episode_sequence_regular")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class EpisodeSequenceRegularEntity extends BaseEntity {
    private String name;
    private String regex;
    @Column("ep_group")
    private EpisodeGroup epGroup;
    private Float sequence;
    private Integer priority;
    private String description;
    private Boolean enabled;
}
