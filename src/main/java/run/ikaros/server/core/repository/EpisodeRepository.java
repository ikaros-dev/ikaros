package run.ikaros.server.core.repository;


import java.util.List;
import run.ikaros.server.entity.EpisodeEntity;

import java.util.Optional;

/**
 * @author li-guohao
 */
public interface EpisodeRepository extends BaseRepository<EpisodeEntity, Long> {

    Optional<EpisodeEntity> findBySeq(Long seq);

    List<EpisodeEntity> findBySeasonIdAndStatus(Long seasonId, boolean status);

    List<EpisodeEntity> findBySeasonIdAndSeqAndStatus(Long seasonId, Long seq, boolean status);
}
