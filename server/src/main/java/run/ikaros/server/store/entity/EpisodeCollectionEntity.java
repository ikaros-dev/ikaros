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
public class EpisodeCollectionEntity {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("episode_id")
    private Long episodeId;
    /**
     * 是否已经看过.
     */
    private Boolean finish;
    /**
     * 观看进度，时间戳.
     */
    private Long progress;
    /**
     * 总时长，时间戳.
     */
    private Long duration;
    @Column("update_time")
    private LocalDateTime updateTime;
}
