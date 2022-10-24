package run.ikaros.server.repository;


import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.repository.BaseRepository;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface EpisodeRepository extends BaseRepository<EpisodeEntity, Long> {

    Optional<EpisodeEntity> findBySeq(Long seq);
}
