package run.ikaros.server.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

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
@TableName("episode_list_episode")
public class EpisodeListEpisode implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private Object episodeListId;

    private Object episodeId;
}
