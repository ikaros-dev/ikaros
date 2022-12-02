package run.ikaros.server.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.FileEntity;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EpisodeDTO extends EpisodeEntity {
    private FileEntity file;
}
