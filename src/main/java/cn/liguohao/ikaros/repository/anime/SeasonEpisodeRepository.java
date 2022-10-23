package cn.liguohao.ikaros.repository.anime;


import cn.liguohao.ikaros.model.entity.anime.SeasonEpisodeEntity;
import cn.liguohao.ikaros.repository.BaseRepository;
import java.util.List;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface SeasonEpisodeRepository extends BaseRepository<SeasonEpisodeEntity> {
    List<SeasonEpisodeEntity> findBySeasonIdAndStatus(Long seasonId, boolean status);

    List<SeasonEpisodeEntity> findBySeasonId(Long seasonId);

    Optional<SeasonEpisodeEntity> findBySeasonIdAndEpisodeId(Long seasonId, Long episodeId);
}
