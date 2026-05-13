package run.ikaros.server.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@TableName("episode_list_collection")
public class EpisodeListCollection implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private Object userId;

    private Object episodeListId;

    private LocalDateTime updateTime;
}
