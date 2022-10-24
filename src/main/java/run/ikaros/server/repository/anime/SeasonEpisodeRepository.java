package run.ikaros.server.repository.anime;


import run.ikaros.server.entity.anime.SeasonEpisodeEntity;
import run.ikaros.server.repository.BaseRepository;
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
