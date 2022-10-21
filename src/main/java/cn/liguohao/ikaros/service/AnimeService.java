package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.constants.AnimeConstants;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.common.kit.DateKit;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.bgmtv.BgmTvConstants;
import cn.liguohao.ikaros.model.bgmtv.BgmTvEpisode;
import cn.liguohao.ikaros.model.bgmtv.BgmTvEpisodeType;
import cn.liguohao.ikaros.model.bgmtv.BgmTvSubject;
import cn.liguohao.ikaros.model.bgmtv.BgmTvTag;
import cn.liguohao.ikaros.model.dto.AnimeDTO;
import cn.liguohao.ikaros.model.dto.EpisodeDTO;
import cn.liguohao.ikaros.model.dto.SeasonDTO;
import cn.liguohao.ikaros.model.entity.FileEntity;
import cn.liguohao.ikaros.model.entity.TagEntity;
import cn.liguohao.ikaros.model.entity.anime.AnimeEntity;
import cn.liguohao.ikaros.model.entity.anime.AnimeSeasonEntity;
import cn.liguohao.ikaros.model.entity.anime.AnimeTagEntity;
import cn.liguohao.ikaros.model.entity.anime.EpisodeEntity;
import cn.liguohao.ikaros.model.entity.anime.SeasonEntity;
import cn.liguohao.ikaros.model.entity.anime.SeasonEpisodeEntity;
import cn.liguohao.ikaros.repository.TagRepository;
import cn.liguohao.ikaros.repository.anime.AnimeRepository;
import cn.liguohao.ikaros.repository.anime.AnimeSeasonRepository;
import cn.liguohao.ikaros.repository.anime.AnimeTagRepository;
import cn.liguohao.ikaros.repository.anime.EpisodeRepository;
import cn.liguohao.ikaros.repository.anime.SeasonEpisodeRepository;
import cn.liguohao.ikaros.repository.anime.SeasonRepository;
import java.util.Date;
import java.util.List;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class AnimeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeService.class);

    private final AnimeRepository animeRepository;
    private final AnimeSeasonRepository animeSeasonRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonEpisodeRepository seasonEpisodeRepository;
    private final EpisodeRepository episodeRepository;
    private final UserService userService;
    private final BgmTvService bgmTvService;
    private final TagRepository tagRepository;
    private final AnimeTagRepository animeTagRepository;

    public AnimeService(AnimeRepository animeRepository,
                        AnimeSeasonRepository animeSeasonRepository,
                        SeasonRepository seasonRepository,
                        SeasonEpisodeRepository seasonEpisodeRepository,
                        EpisodeRepository episodeRepository,
                        UserService userService, BgmTvService bgmTvService,
                        TagRepository tagRepository, AnimeTagRepository animeTagRepository) {
        this.animeRepository = animeRepository;
        this.animeSeasonRepository = animeSeasonRepository;
        this.seasonRepository = seasonRepository;
        this.seasonEpisodeRepository = seasonEpisodeRepository;
        this.episodeRepository = episodeRepository;
        this.userService = userService;
        this.bgmTvService = bgmTvService;
        this.tagRepository = tagRepository;
        this.animeTagRepository = animeTagRepository;
    }


    @Deprecated
    public AnimeDTO addAnime(AnimeDTO animeDTO) {
        Assert.notNull(animeDTO, "'anime' must not be null.");

        final Date now = new Date();
        final Long loginUserUid = userService.getCurrentLoginUserUid();

        Assert.notNull(animeDTO.getTitle(), "'anime#getTitle' must not be null.");
        AnimeEntity animeEntity = new AnimeEntity();
        animeEntity.setTimeAndUidWhenCreate(now, loginUserUid);
        BeanKit.copyProperties(animeDTO, animeEntity);
        animeEntity = animeRepository.saveAndFlush(animeEntity);

        Long animeId = animeEntity.getId();
        Assert.notNull(animeId, "'animeId' must not be null");
        animeDTO.setId(animeId);

        List<SeasonDTO> seasonDTOS = animeDTO.getSeasons();
        Assert.notNull(seasonDTOS, "'seasons' must not be null");
        for (SeasonDTO seasonDTO : seasonDTOS) {
            SeasonEntity seasonEntity = new SeasonEntity();
            seasonEntity.setTimeAndUidWhenCreate(now, loginUserUid);
            BeanKit.copyProperties(seasonDTO, seasonEntity);
            seasonEntity = seasonRepository.saveAndFlush(seasonEntity);


            Long seasonId = seasonEntity.getId();
            Assert.notNull(seasonId, "'seasonId' must not be null");
            seasonDTO.setId(seasonId);

            AnimeSeasonEntity animeSeasonEntity = new AnimeSeasonEntity();
            animeSeasonEntity.setTimeAndUidWhenCreate(now, loginUserUid);
            animeSeasonEntity.setAnimeId(animeId)
                .setSeasonId(seasonId);
            animeSeasonRepository.saveAndFlush(animeSeasonEntity);


            List<EpisodeDTO> episodeDTOS = seasonDTO.getEpisodes();
            Assert.notNull(episodeDTOS, "'episodes' must not be null");
            for (EpisodeDTO episodeDTO : episodeDTOS) {
                EpisodeEntity episodeEntity = new EpisodeEntity();
                episodeEntity.setTimeAndUidWhenCreate(now, loginUserUid);
                BeanKit.copyProperties(episodeDTO, episodeEntity);
                episodeEntity = episodeRepository.saveAndFlush(episodeEntity);

                // PS: 文件上传是在保存元数据之前的，这里应该是获取到文件ID的
                Long episodeId = episodeEntity.getId();
                Assert.notNull(episodeId, "'episodeId' must not be null");
                episodeDTO.setId(episodeId);

                SeasonEpisodeEntity seasonEpisodeEntity = new SeasonEpisodeEntity();
                seasonEpisodeEntity.setTimeAndUidWhenCreate(now, loginUserUid);
                seasonEpisodeEntity.setEpisodeId(episodeId)
                    .setSeasonId(seasonId);
                seasonEpisodeRepository.saveAndFlush(seasonEpisodeEntity);
            }

        }

        return animeDTO;
    }

    public boolean existById(Long id) {
        Assert.isPositive(id, "'id' must be positive");
        return animeRepository.existsByIdAndStatus(id, true);
    }

    public AnimeEntity findById(Long id) throws RecordNotFoundException {
        Assert.isPositive(id, "'id' must be positive");
        if (!existById(id)) {
            throw new RecordNotFoundException("anime entity not exist, id=" + id);
        }
        return animeRepository.findByIdAndStatus(id, true).get();
    }

    public AnimeDTO saveAnimeDTO(AnimeDTO animeDTO) {
        // todo save animeDTO
        return null;
    }

    public AnimeDTO findAnimeDTOById(Long id) throws RecordNotFoundException {
        AnimeEntity animeEntity = findById(id);
        return findAnimeDTOByAnimeEntity(animeEntity);
    }

    private AnimeDTO findAnimeDTOByAnimeEntity(AnimeEntity animeEntity) {
        // todo findAnimeDTOByAnimeEntity

        return null;
    }


    public AnimeDTO reqBgmtvBangumiMetadata(Long subjectId) {
        try {
            // 直接返回已经存在的番剧数据
            Long id = animeRepository.findIdByBgmtvIdAndStatus(subjectId, true);
            AnimeDTO animeDTO = findAnimeDTOById(id);
            LOGGER.debug("anime exist, return this anime, subjectId={}, animeId={}", subjectId, id);
            return animeDTO;
        } catch (RecordNotFoundException e) {
            // 创建新的动漫对象
            LOGGER.debug("anime not exist, will  create a new, subjectId={}", subjectId);

            // 获取动漫信息
            BgmTvSubject bgmTvSubject = bgmTvService.getSubject(subjectId);
            if (bgmTvSubject == null) {
                LOGGER.error("request bgmtv fail, response null subject");
                return null;
            }

            AnimeEntity animeEntity = new AnimeEntity();
            String coverUrl = bgmTvSubject.getImages().getLarge();
            FileEntity coverFileEntity = bgmTvService.downloadCover(coverUrl);
            animeEntity.setCoverUrl(coverFileEntity.getUrl())
                .setAirTime(
                    DateKit.parseDateStr(bgmTvSubject.getDate(), BgmTvConstants.DATE_PATTERN))
                .setPlatform(bgmTvSubject.getPlatform())
                .setOverview(bgmTvSubject.getSummary())
                .setTitle(bgmTvSubject.getNameCn())
                .setOriginalTitle(bgmTvSubject.getName());
            animeEntity = animeRepository.saveAndFlush(animeEntity);

            for (BgmTvTag bgmTvTag : bgmTvSubject.getTags()) {
                TagEntity tagEntity =
                    tagRepository.saveAndFlush(new TagEntity().setName(bgmTvTag.getName()));
                AnimeTagEntity animeTagEntity = new AnimeTagEntity()
                    .setAnimeId(animeEntity.getId())
                    .setTagId(tagEntity.getId());
                animeTagRepository.saveAndFlush(animeTagEntity);
            }

            // 获取动漫剧集信息
            List<BgmTvEpisode> bgmTvEpisodes =
                bgmTvService.getEpisodesBySubjectId(subjectId, BgmTvEpisodeType.POSITIVE);

            new SeasonEntity()
                .setType(AnimeConstants.DEFAULT_SEASON_TYPE);

            for (BgmTvEpisode bgmTvEpisode : bgmTvEpisodes) {
                Date airDate =
                    DateKit.parseDateStr(bgmTvEpisode.getAirDate(), BgmTvConstants.DATE_PATTERN);
                EpisodeEntity episodeEntity = new EpisodeEntity()
                    .setSeq(bgmTvEpisode.getEp().longValue())
                    .setTitle(bgmTvEpisode.getNameCn())
                    .setOriginalTitle(bgmTvEpisode.getName())
                    .setAirTime(airDate)
                    .setOverview(bgmTvEpisode.getDesc())
                    .setDuration(bgmTvEpisode.getDurationSeconds().longValue());
                episodeEntity = episodeRepository.saveAndFlush(episodeEntity);

            }


            return null;
        }
    }


}
