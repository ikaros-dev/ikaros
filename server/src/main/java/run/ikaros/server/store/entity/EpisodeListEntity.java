package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "episode_list")
@EqualsAndHashCode(callSuper = true)
public class EpisodeListEntity extends BaseEntity {
    @Id
    private Long id;
    private String name;
    @Column("name_cn")
    private String nameCn;
    private String cover;
    /**
     * Not Safe/Suitable For Work.
     */
    private Boolean nsfw;
    private String description;
}
