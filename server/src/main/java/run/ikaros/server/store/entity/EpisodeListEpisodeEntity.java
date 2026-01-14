package run.ikaros.server.store.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "episode_list_episode")
@Accessors(chain = true)
public class EpisodeListEpisodeEntity {
    @Id
    private Long id;
    @Column("episode_list_id")
    private Long episodeListId;
    @Column("episode_id")
    private Long episodeId;
    private UUID uuid;
}
