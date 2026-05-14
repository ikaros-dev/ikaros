package run.ikaros.server.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <p>
 * 
 * </p>
 *
 * @author chivehao
 * @since 2026-05-13
 */
@Getter
@Setter
@ToString
@TableName("episode_collection")
public class EpisodeCollection implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private UUID userId;

    private UUID subjectId;

    private UUID episodeId;

    private Boolean finish;

    private Long progress;

    private Long duration;

    private LocalDateTime updateTime;
}
