package cn.liguohao.ikaros.repository.anime;


import cn.liguohao.ikaros.model.entity.anime.AnimeSeasonEntity;
import cn.liguohao.ikaros.repository.BaseRepository;
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
