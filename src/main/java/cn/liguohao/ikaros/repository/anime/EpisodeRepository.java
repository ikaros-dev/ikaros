package cn.liguohao.ikaros.repository.anime;


import cn.liguohao.ikaros.model.entity.anime.EpisodeEntity;
import cn.liguohao.ikaros.repository.BaseRepository;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface EpisodeRepository extends BaseRepository<EpisodeEntity> {

    Optional<EpisodeEntity> findBySeq(Long seq);
}
