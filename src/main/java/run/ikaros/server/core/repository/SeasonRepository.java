package run.ikaros.server.core.repository;


import run.ikaros.server.entity.SeasonEntity;

/**
 * @author li-guohao
 */
public interface SeasonRepository extends BaseRepository<SeasonEntity, Long> {

    SeasonEntity findSeasonEntityByTitleLikeAndStatus(String title, Boolean status);

    SeasonEntity findSeasonEntityByTitleCnLikeAndStatus(String titleCn, Boolean status);

}
