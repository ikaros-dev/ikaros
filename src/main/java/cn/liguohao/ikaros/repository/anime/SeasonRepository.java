package cn.liguohao.ikaros.repository.anime;


import cn.liguohao.ikaros.model.entity.anime.SeasonEntity;
import cn.liguohao.ikaros.repository.BaseRepository;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface SeasonRepository extends BaseRepository<SeasonEntity> {
    Optional<SeasonEntity> findByTypeAndOriginalTitle(Integer type, String originalTitle);

    Optional<SeasonEntity> findByIdAndTypeAndOriginalTitle(Long id, SeasonEntity.Type type,
                                                           String originalTitle);
}
