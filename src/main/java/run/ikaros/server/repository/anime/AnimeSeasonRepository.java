package run.ikaros.server.repository.anime;


import run.ikaros.server.entity.anime.AnimeSeasonEntity;
import run.ikaros.server.repository.BaseRepository;
import java.util.List;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface AnimeSeasonRepository extends BaseRepository<AnimeSeasonEntity> {

    List<AnimeSeasonEntity> findByAnimeIdAndStatus(Long animeId, boolean status);

    List<AnimeSeasonEntity> findByAnimeId(Long animeId);

    Optional<AnimeSeasonEntity> findByAnimeIdAndSeasonId(Long animeId, Long seasonId);
}
