package run.ikaros.server.repository.anime;


import run.ikaros.server.entity.anime.SeasonEntity;
import run.ikaros.server.repository.BaseRepository;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface SeasonRepository extends BaseRepository<SeasonEntity> {
    Optional<SeasonEntity> findByTypeAndOriginalTitle(Integer type, String originalTitle);

    Optional<SeasonEntity> findByIdAndTypeAndOriginalTitle(Long id, SeasonEntity.Type type,
                                                           String originalTitle);
}
