package run.ikaros.server.repository.anime;


import run.ikaros.server.entity.anime.EpisodeEntity;
import run.ikaros.server.repository.BaseRepository;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface EpisodeRepository extends BaseRepository<EpisodeEntity> {

    Optional<EpisodeEntity> findBySeq(Long seq);
}
