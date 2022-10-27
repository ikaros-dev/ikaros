package run.ikaros.server.repository;


import java.util.List;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.repository.BaseRepository;
import java.util.Optional;

/**
 * @author li-guohao
 */
public interface EpisodeRepository extends BaseRepository<EpisodeEntity, Long> {

    Optional<EpisodeEntity> findBySeq(Long seq);

    List<EpisodeEntity> findBySeasonIdAndStatus(Long seasonId, boolean status);
}
