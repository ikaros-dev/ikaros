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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "episode_list_collection")
@Accessors(chain = true)
public class EpisodeListCollectionEntity {
    @Id
    private UUID id;
    @Column("user_id")
    private UUID userId;
    @Column("episode_list_id")
    private UUID episodeListId;
    @Column("update_time")
    private LocalDateTime updateTime;
}
