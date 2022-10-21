package cn.liguohao.ikaros.repository.anime;


import cn.liguohao.ikaros.model.entity.anime.AnimeEntity;
import cn.liguohao.ikaros.repository.BaseRepository;
import java.util.List;

/**
 * @author li-guohao
 */
public interface AnimeRepository extends BaseRepository<AnimeEntity> {
    List<AnimeEntity> findByBgmtvIdAndStatus(Long bgmtvId, Boolean status);

}
