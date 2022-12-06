package run.ikaros.server.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.UserSubscribeEntity;

import java.util.List;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnimeDTO extends AnimeEntity {

    private boolean isSub;
    private UserSubscribeEntity subscribe;
    private List<SeasonDTO> seasons;

}
