package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.SeasonRepository;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.enums.FileType;
import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.event.EpisodeUrlUpdateEvent;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.exceptions.RegexMatchingException;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.exceptions.SeasonEpisodeMatchingFailException;
import run.ikaros.server.model.dto.EpisodeDTO;
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.params.SeasonMatchingEpParams;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.FileUtils;
import run.ikaros.server.utils.RegexUtils;
import run.ikaros.server.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author li-guohao
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class SeasonServiceImpl
    extends AbstractCrudService<SeasonEntity, Long>
    implements SeasonService, ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonServiceImpl.class);

    private final SeasonRepository seasonRepository;
    private final FileService fileService;
    private final EpisodeService episodeService;

    private ApplicationContext applicationContext;

    public SeasonServiceImpl(SeasonRepository seasonRepository, FileService fileService,
                             EpisodeService episodeService) {
        super(seasonRepository);
        this.seasonRepository = seasonRepository;
        this.fileService = fileService;
        this.episodeService = episodeService;
    }


    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
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
    public SeasonDTO matchingEpisodesUrlByFileIds(
        @Nonnull SeasonMatchingEpParams seasonMatchingEpParams) {
        AssertUtils.notNull(seasonMatchingEpParams, "seasonMatchingEpParams");
        Long seasonId = seasonMatchingEpParams.getSeasonId();
        List<Long> fileIdList = seasonMatchingEpParams.getFileIdList();
        AssertUtils.notNull(seasonId, "seasonId");
        AssertUtils.notNull(fileIdList, "fileIdList");

        final List<EpisodeEntity> episodeEntities = new ArrayList<>();
        for (Long fileId : fileIdList) {
            FileEntity fileEntity = fileService.findById(fileId);
            if (fileEntity == null) {
                continue;
            }
            final String fileName = fileEntity.getName();
            FileType fileType = FileUtils.parseTypeByPostfix(FileUtils.parseFileName(fileName));
            if (fileType != FileType.VIDEO) {
                continue;
            }

            Long episodeSeq = null;
            try {
                episodeSeq = RegexUtils.getFileNameTagEpSeq(fileName);
            } catch (RegexMatchingException regexMatchingException) {
                LOGGER.warn("fail matching seq by file name={}, exception msg={}", fileName,
                    regexMatchingException.getMessage());
                continue;
            }

            List<EpisodeEntity> episodeEntityList =
                episodeService.findBySeasonIdAndSeq(seasonId, episodeSeq);
            if (episodeEntityList.isEmpty()) {
                LOGGER.warn("episode records not found where seasonId={} and seq={}",
                    seasonId, episodeSeq);
                continue;
            }
            EpisodeEntity episodeEntity = episodeEntityList.get(0);
            String newEpisodeUrl = fileEntity.getUrl();
            if (StringUtils.isNotBlank(newEpisodeUrl)) {
                String oldEpisodeUrl = episodeEntity.getUrl();
                if (!newEpisodeUrl.equalsIgnoreCase(oldEpisodeUrl)) {
                    EpisodeUrlUpdateEvent episodeUrlUpdateEvent =
                        new EpisodeUrlUpdateEvent(this,
                            episodeEntity.getId(), oldEpisodeUrl, newEpisodeUrl,
                            fileEntity.getName(), seasonMatchingEpParams.getIsNotify());
                    applicationContext.publishEvent(episodeUrlUpdateEvent);
                }
                episodeEntity.setUrl(newEpisodeUrl);
                episodeService.save(episodeEntity);
                LOGGER.debug("matching file and episode success,  "
                        + "fileId={} fileName={}, episodeTitle={} episodeSeq={} episodeUrl={}",
                    fileId, fileEntity.getName(),
                    episodeEntity.getTitle(), episodeSeq, newEpisodeUrl);
            }
            episodeEntities.add(episodeEntity);
        }

        SeasonDTO seasonDTO = new SeasonDTO();
        SeasonEntity seasonEntity = getById(seasonId);
        if (seasonEntity == null || !seasonEntity.getStatus()) {
            throw new SeasonEpisodeMatchingFailException("season not exist, id=" + seasonId);
        }

        BeanUtils.copyProperties(seasonEntity, seasonDTO);
        seasonDTO.setEpisodes(episodeEntities.stream().flatMap(
            (Function<EpisodeEntity, Stream<EpisodeDTO>>) episodeEntity -> {
                EpisodeDTO episodeDTO = new EpisodeDTO();
                BeanUtils.copyProperties(episodeEntity, episodeDTO);
                return Stream.of(episodeDTO);
            }).collect(Collectors.toList()));

        return seasonDTO;
    }

    @Override
    public void updateEpisodeUrlByFileEntity(@Nonnull FileEntity fileEntity,
                                             @Nonnull Boolean isNotify) {
        AssertUtils.notNull(fileEntity, "fileEntity");
        final String originalFileName = fileEntity.getName();
        SeasonEntity seasonEntity = null;
        // 根据文件名称英文查询 如未查到则根据中文查询
        String title = null;
        try {
            title = RegexUtils.getMatchingChineseStrWithoutTag(originalFileName);
        } catch (RegexMatchingException matchingException) {
            try {
                title = RegexUtils.getMatchingEnglishStrWithoutTag(originalFileName);
            } catch (RegexMatchingException exception) {
                LOGGER.warn("matching fail, skip for fileName={}, exception msg={}",
                    originalFileName, exception.getMessage());
                return;
            }
        }

        Optional<SeasonEntity> seasonEntityOptional =
            seasonRepository.findOne(Example.of(new SeasonEntity().setTitle(title)));
        if (seasonEntityOptional.isEmpty()) {
            String chineseName = RegexUtils.getMatchingChineseStrWithoutTag(originalFileName);
            Optional<SeasonEntity> chineseSeasonEntityOptional =
                seasonRepository.findOne(Example.of(new SeasonEntity().setTitleCn(chineseName)));
            seasonEntity = chineseSeasonEntityOptional.orElse(null);
        } else {
            seasonEntity = seasonEntityOptional.get();
        }

        if (seasonEntity == null) {
            LOGGER.warn("matching season fail by file name={}", fileEntity.getName());
            return;
        }

        Long seasonId = seasonEntity.getId();
        List<EpisodeEntity> episodeEntityList = episodeService.findBySeasonId(seasonId);
        Long seq = RegexUtils.getFileNameTagEpSeq(originalFileName);
        Optional<EpisodeEntity> episodeEntityOptional = episodeEntityList.stream()
            .filter(episodeEntity -> seq.equals(episodeEntity.getSeq()))
            .findFirst();
        if (episodeEntityOptional.isEmpty()) {
            LOGGER.warn("matching episode fail for seasonId={}, fileUrl={}",
                seasonId, fileEntity.getUrl());
            return;
        }
        EpisodeEntity episodeEntity = episodeEntityOptional.get();
        if (StringUtils.isNotBlank(fileEntity.getUrl())) {
            String oldEpisodeUrl = episodeEntity.getUrl();
            String newEpisodeUrl = fileEntity.getUrl();
            if (StringUtils.isNotBlank(newEpisodeUrl)
                && !newEpisodeUrl.equalsIgnoreCase(oldEpisodeUrl)) {
                EpisodeUrlUpdateEvent episodeUrlUpdateEvent =
                    new EpisodeUrlUpdateEvent(this, episodeEntity.getId(), oldEpisodeUrl,
                        newEpisodeUrl, fileEntity.getName(), isNotify);
                applicationContext.publishEvent(episodeUrlUpdateEvent);
            }
            episodeEntity.setUrl(fileEntity.getUrl());
            episodeService.save(episodeEntity);
            LOGGER.debug(
                "update episode url by file entity success, originalFileName={}, newUrl={}",
                originalFileName, fileEntity.getUrl());
        }

    }

    @Nullable
    @Override
    public List<SeasonEntity> findSeasonEntityByTitleLike(@Nonnull String title) {
        AssertUtils.notBlank(title, "title");
        return seasonRepository
            .findSeasonEntityByTitleLikeAndStatus(StringUtils.addLikeChar(title), true);
    }

    @Nullable
    @Override
    public List<SeasonEntity> findSeasonEntityByTitleCnLike(@Nonnull String titleCn) {
        AssertUtils.notBlank(titleCn, "titleCn");
        return seasonRepository
            .findSeasonEntityByTitleCnLikeAndStatus(StringUtils.addLikeChar(titleCn), true);
    }

    @Nonnull
    @Override
    public SeasonDTO matchingEpisodeUrlByFileId(
        @Nonnull SeasonMatchingEpParams seasonMatchingEpParams) {
        AssertUtils.notNull(seasonMatchingEpParams, "seasonMatchingEpParams");
        Long episodeId = seasonMatchingEpParams.getEpisodeId();
        Long fileId = seasonMatchingEpParams.getFileId();
        AssertUtils.isPositive(episodeId, "episode id");
        AssertUtils.isPositive(fileId, "file id");
        final SeasonDTO seasonDTO = new SeasonDTO();


        EpisodeEntity episodeEntity = episodeService.getById(episodeId);
        if (episodeEntity == null || episodeEntity.getSeasonId() == null) {
            throw new RecordNotFoundException("not found episode for id=" + episodeId);
        }

        FileEntity fileEntity = fileService.findById(fileId);
        if (fileEntity == null) {
            throw new RecordNotFoundException("not found file for id=" + fileId);
        }
        Long fileNameTagEpSeq = RegexUtils.getFileNameTagEpSeq(fileEntity.getName());
        if (!Objects.equals(episodeEntity.getSeq(), fileNameTagEpSeq)) {
            throw new RuntimeIkarosException("current episode seq and file name seq not matching"
                + ", ep seq:" + episodeEntity.getSeq() + "  file name seq: " + fileNameTagEpSeq);
        }

        String newEpisodeUrl = fileEntity.getUrl();
        if (StringUtils.isNotBlank(newEpisodeUrl)) {
            String oldEpisodeUrl = episodeEntity.getUrl();
            if (!newEpisodeUrl.equalsIgnoreCase(oldEpisodeUrl)) {
                EpisodeUrlUpdateEvent episodeUrlUpdateEvent =
                    new EpisodeUrlUpdateEvent(this, episodeEntity.getId(), oldEpisodeUrl,
                        newEpisodeUrl, fileEntity.getName(), seasonMatchingEpParams.getIsNotify());
                applicationContext.publishEvent(episodeUrlUpdateEvent);
            }
            episodeEntity.setUrl(newEpisodeUrl);
            episodeEntity = episodeService.save(episodeEntity);
            LOGGER.debug("matching file and episode success,  "
                    + "fileId={} fileName={}, episodeTitle={} episodeSeq={} episodeUrl={}",
                fileId, fileEntity.getName(),
                episodeEntity.getTitle(), episodeEntity.getSeq(), newEpisodeUrl);
        }

        SeasonEntity seasonEntity = getById(episodeEntity.getSeasonId());
        if (seasonEntity == null) {
            throw new RecordNotFoundException("not found season for episode id=" + episodeId);
        }
        BeanUtils.copyProperties(seasonEntity, seasonDTO);
        seasonDTO.setEpisodes(episodeService.findDtoListBySeasonId(seasonEntity.getId()));
        return seasonDTO;
    }

    @Nonnull
    @Override
    public List<SeasonDTO> findDtoListByAnimeId(@Nonnull Long id) {
        AssertUtils.isPositive(id, "anime id");
        return findByAnimeId(id)
            .stream()
            .flatMap((Function<SeasonEntity, Stream<SeasonDTO>>) seasonEntity -> {
                SeasonDTO seasonDTO = new SeasonDTO();
                BeanUtils.copyProperties(seasonEntity, seasonDTO);
                seasonDTO.setEpisodes(
                    episodeService.findDtoListBySeasonId(seasonEntity.getId()));
                return Stream.of(seasonDTO);
            }).collect(Collectors.toList());
    }


}
