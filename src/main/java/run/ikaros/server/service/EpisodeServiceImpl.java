package run.ikaros.server.service;

import java.util.List;
import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.core.repository.BaseRepository;
import run.ikaros.server.core.repository.EpisodeRepository;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.utils.AssertUtils;

/**
 * @author li-guohao
 */
@Service
public class EpisodeServiceImpl
    extends AbstractCrudService<EpisodeEntity, Long>
    implements EpisodeService {

    private final EpisodeRepository episodeRepository;

    public EpisodeServiceImpl(
        BaseRepository<EpisodeEntity, Long> baseRepository, EpisodeRepository episodeRepository) {
        super(baseRepository);
        this.episodeRepository = episodeRepository;
    }


    @Nonnull
    @Override
    public List<EpisodeEntity> findBySeasonId(@Nonnull Long seasonId) {
        AssertUtils.notNull(seasonId, "seasonId");
        return episodeRepository.findBySeasonIdAndStatus(seasonId, true);
    }
}
