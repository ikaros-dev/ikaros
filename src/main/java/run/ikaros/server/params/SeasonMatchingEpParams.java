package run.ikaros.server.params;

import lombok.Data;

import java.util.List;

/**
 * @author li-guohao
 */
@Data
public class SeasonMatchingEpParams {
    private Long seasonId;
    private List<Long> fileIdList;
    private Long episodeId;
    private Long fileId;
    private Boolean isNotify = false;

}
