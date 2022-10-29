package run.ikaros.server.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * @author li-guohao
 */
public class SeasonMatchingEpParams {
    @JsonProperty("season_id")
    private Long seasonId;

    @JsonProperty("file_id_list")
    private List<Long> fileIdList;

    public Long getSeasonId() {
        return seasonId;
    }

    public SeasonMatchingEpParams setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
        return this;
    }

    public List<Long> getFileIdList() {
        return fileIdList;
    }

    public SeasonMatchingEpParams setFileIdList(List<Long> fileIdList) {
        this.fileIdList = fileIdList;
        return this;
    }
}
