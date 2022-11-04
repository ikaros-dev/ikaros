package run.ikaros.server.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.persistence.Tuple;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.exceptions.SeasonEpisodeMatchingFailException;
import run.ikaros.server.model.dto.EpisodeDTO;
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.params.SeasonMatchingEpParams;
import run.ikaros.server.repository.SeasonRepository;
import run.ikaros.server.service.EpisodeService;
import run.ikaros.server.service.FileService;
import run.ikaros.server.service.SeasonService;
import run.ikaros.server.service.base.AbstractCrudService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.JsonUtils;

/**
 * @author li-guohao
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class SeasonServiceImpl
    extends AbstractCrudService<SeasonEntity, Long>
    implements SeasonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonServiceImpl.class);

    private final SeasonRepository seasonRepository;
    private final FileService fileService;
    private final EpisodeService episodeService;

    public SeasonServiceImpl(SeasonRepository seasonRepository, FileService fileService,
                             EpisodeService episodeService) {
        super(seasonRepository);
        this.seasonRepository = seasonRepository;
        this.fileService = fileService;
        this.episodeService = episodeService;
    }

    @Nonnull
    @Override
    public List<SeasonEntity> findByAnimeId(@Nonnull Long animeId) {
        AssertUtils.isPositive(animeId, "animeId");
        SeasonEntity seasonEntityExample
            = new SeasonEntity()
            .setType(null)
            .setAnimeId(animeId);
        seasonEntityExample.setStatus(true);
        return listAll(Example.of(seasonEntityExample));
    }

    @Nonnull
    @Override
    public List<String> finSeasonTypes() {
        return Arrays.stream(SeasonType.values())
            .flatMap((Function<SeasonType, Stream<String>>) seasonType
                -> Stream.of(seasonType.name()))
            .toList();
    }

    @Nonnull
    @Override
    public SeasonDTO matchingEpisodeUrlByFileIds(
        @Nonnull SeasonMatchingEpParams seasonMatchingEpParams) {
        AssertUtils.notNull(seasonMatchingEpParams, "seasonMatchingEpParams");
        Long seasonId = seasonMatchingEpParams.getSeasonId();
        List<Long> fileIdList = seasonMatchingEpParams.getFileIdList();
        AssertUtils.notNull(seasonId, "season_id");
        AssertUtils.notNull(fileIdList, "file_id_list");

        List<EpisodeEntity> episodeEntityList = episodeService.findBySeasonId(seasonId)
            .stream()
            .sorted((ep1, ep2) -> Objects.equals(ep1.getSeq(), ep2.getSeq()) ? 0 :
                (ep1.getSeq() > ep2.getSeq() ? 1 : -1)).toList();


        final Map<Long, FileEntity> seqFileEntityMap = new HashMap<>();

        fileIdList.stream()
            .flatMap((Function<Long, Stream<FileEntity>>) id
                -> Stream.of(fileService.getById(id)))
            .forEach(fileEntity
                -> seqFileEntityMap.put(
                fileService.getEpisodeSeqFromName(fileEntity.getName()),
                fileEntity));

        if (seqFileEntityMap.isEmpty()) {
            throw new SeasonEpisodeMatchingFailException("file not found by file id list: "
                + JsonUtils.obj2Json(fileIdList));
        }

        episodeService.findBySeasonId(seasonId).forEach(episodeEntity -> {
            if (seqFileEntityMap.containsKey(episodeEntity.getSeq())) {
                FileEntity fileEntity = seqFileEntityMap.get(episodeEntity.getSeq());
                episodeEntity.setUrl(fileEntity.getUrl());
                episodeEntity = episodeService.save(episodeEntity);
            }
        });

        SeasonDTO seasonDTO = new SeasonDTO();
        SeasonEntity seasonEntity = getById(seasonId);
        if (!seasonEntity.getStatus()) {
            throw new SeasonEpisodeMatchingFailException("season not exist, id=" + seasonId);
        }

        BeanUtils.copyProperties(seasonEntity, seasonDTO);
        seasonDTO.setEpisodes(episodeEntityList.stream().flatMap(
            (Function<EpisodeEntity, Stream<EpisodeDTO>>) episodeEntity -> {
                EpisodeDTO episodeDTO = new EpisodeDTO();
                BeanUtils.copyProperties(episodeEntity, episodeDTO);
                return Stream.of(episodeDTO);
            }).collect(Collectors.toList()));

        return seasonDTO;
    }


}
