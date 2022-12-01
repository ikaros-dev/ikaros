package run.ikaros.server.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.core.repository.BaseRepository;
import run.ikaros.server.core.repository.EpisodeRepository;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.model.dto.EpisodeDTO;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.StringUtils;

/**
 * @author li-guohao
 */
@Service
public class EpisodeServiceImpl
    extends AbstractCrudService<EpisodeEntity, Long>
    implements EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final FileService fileService;

    public EpisodeServiceImpl(
        BaseRepository<EpisodeEntity, Long> baseRepository, EpisodeRepository episodeRepository,
        FileService fileService) {
        super(baseRepository);
        this.episodeRepository = episodeRepository;
        this.fileService = fileService;
    }


    @Nonnull
    @Override
    public List<EpisodeEntity> findBySeasonId(@Nonnull Long seasonId) {
        AssertUtils.notNull(seasonId, "seasonId");
        return episodeRepository.findBySeasonIdAndStatus(seasonId, true);
    }

    @Override
    public List<EpisodeEntity> findBySeasonIdAndSeq(@Nonnull Long seasonId, @Nonnull Long seq) {
        AssertUtils.isPositive(seasonId, "season id");
        AssertUtils.isPositive(seq, "episode seq");
        return episodeRepository.findBySeasonIdAndSeqAndStatus(seasonId, seq, true);
    }

    @Nonnull
    @Override
    public List<EpisodeDTO> findDtoListBySeasonId(@Nonnull Long id) {
        return findBySeasonId(id)
            .stream()
            .flatMap((Function<EpisodeEntity, Stream<EpisodeDTO>>) ee -> {
                EpisodeDTO episodeDTO = new EpisodeDTO();
                String url = ee.getUrl();
                if (StringUtils.isNotBlank(url)) {
                    Optional<FileEntity> fileEntityOptional = fileService.findByUrl(url);
                    episodeDTO.setFile(fileEntityOptional.orElse(null));
                }
                BeanUtils.copyProperties(ee, episodeDTO);
                return Stream.of(episodeDTO);
            }).collect(Collectors.toList());
    }
}
