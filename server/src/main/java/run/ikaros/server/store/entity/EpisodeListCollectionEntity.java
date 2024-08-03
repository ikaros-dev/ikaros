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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "episode_collection")
@Accessors(chain = true)
public class EpisodeListCollectionEntity {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("episode_list_id")
    private Long episodeListId;
    @Column("update_time")
    private LocalDateTime updateTime;
}
