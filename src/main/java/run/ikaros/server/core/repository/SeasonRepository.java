package run.ikaros.server.core.repository;


import run.ikaros.server.entity.SeasonEntity;

import java.util.List;

/**
 * @author li-guohao
 */
public interface SeasonRepository extends BaseRepository<SeasonEntity, Long> {

    List<SeasonEntity> findSeasonEntityByTitleLikeAndStatus(String title, Boolean status);

    List<SeasonEntity> findSeasonEntityByTitleCnLikeAndStatus(String titleCn, Boolean status);

}
