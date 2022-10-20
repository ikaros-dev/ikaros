package cn.liguohao.ikaros.repository.anime;


import cn.liguohao.ikaros.model.entity.anime.AnimeEntity;
import cn.liguohao.ikaros.repository.BaseRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

/**
 * @author li-guohao
 */
public interface AnimeRepository extends BaseRepository<AnimeEntity> {
    List<AnimeEntity> findByBgmtvIdAndStatus(Long bgmtvId, Boolean status);

    @Query("select id from AnimeEntity where bgmtvId=?1 and status = ?2")
    Long findIdByBgmtvIdAndStatus(Long bgmtvId, Boolean status);
}
