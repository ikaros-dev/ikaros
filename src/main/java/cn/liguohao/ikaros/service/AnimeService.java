package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.common.kit.DateKit;
import cn.liguohao.ikaros.common.result.PagingWrap;
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
import cn.liguohao.ikaros.model.param.SearchAnimeDTOSParams;
import cn.liguohao.ikaros.repository.TagRepository;
import cn.liguohao.ikaros.repository.anime.AnimeRepository;
import cn.liguohao.ikaros.repository.anime.AnimeSeasonRepository;
import cn.liguohao.ikaros.repository.anime.AnimeTagRepository;
import cn.liguohao.ikaros.repository.anime.EpisodeRepository;
import cn.liguohao.ikaros.repository.anime.SeasonEpisodeRepository;
import cn.liguohao.ikaros.repository.anime.SeasonRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    public AnimeEntity save(AnimeEntity animeEntity) {
        Assert.notNull(animeEntity, "'animeEntity' must not be null");
        String originalTitle = animeEntity.getOriginalTitle();
        Assert.notBlank(originalTitle, "'originalTitle' must not be blank");

        if (animeEntity.getAirTime() == null) {
            animeEntity.setAirTime(new Date());
        }

        animeEntity = animeRepository.saveAndFlush(animeEntity);
        return animeEntity;
    }

    public SeasonEntity saveSeasonEntity(Long animeId, SeasonEntity seasonEntity) {
        Assert.notNull(seasonEntity, "'seasonEntity' must not be null");
        Assert.isPositive(animeId, "'animeId' must be positive");

        // save season
        String originalTitle = seasonEntity.getOriginalTitle();
        Assert.notBlank(originalTitle, "'originalTitle' must not be blank");

        Optional<SeasonEntity> seasonEntityOptional =
            seasonRepository.findByIdAndTypeAndOriginalTitle(seasonEntity.getId(),
                seasonEntity.getType(),
                seasonEntity.getOriginalTitle());
        if (seasonEntityOptional.isPresent()) {
            SeasonEntity existSeasonEntity = seasonEntityOptional.get();
            BeanKit.copyProperties(seasonEntity, existSeasonEntity);
            seasonEntity = existSeasonEntity;
        }
        seasonEntity = seasonRepository.saveAndFlush(seasonEntity);

        // save season-anime relation
        Optional<AnimeSeasonEntity> animeSeasonEntityOptional =
            animeSeasonRepository.findByAnimeIdAndSeasonId(animeId, seasonEntity.getId());
        if (animeSeasonEntityOptional.isPresent()) {
            AnimeSeasonEntity animeSeasonEntity = animeSeasonEntityOptional.get();
            animeSeasonEntity.setStatus(true);
            animeSeasonRepository.saveAndFlush(animeSeasonEntity);
        } else {
            animeSeasonRepository.saveAndFlush(new AnimeSeasonEntity()
                .setSeasonId(seasonEntity.getId())
                .setAnimeId(animeId));
        }

        return seasonEntity;
    }


    public void removeAnimeSeason(Long animeId, Long seasonId) throws RecordNotFoundException {
        Assert.isPositive(animeId, "'animeId' must be positive");
        Assert.isPositive(seasonId, "'seasonId' must be positive");

        // update season entity
        Optional<SeasonEntity> seasonEntityOptional = seasonRepository.findById(seasonId);
        if (seasonEntityOptional.isPresent()) {
            SeasonEntity seasonEntity = seasonEntityOptional.get();
            if (seasonEntity.getStatus()) {
                seasonEntity.setStatus(false);
                seasonRepository.saveAndFlush(seasonEntity);
            }
        }

        // update anime season entity
        Optional<AnimeSeasonEntity> animeSeasonEntityOptional =
            animeSeasonRepository.findByAnimeIdAndSeasonId(animeId, seasonId);
        if (animeSeasonEntityOptional.isPresent()) {
            AnimeSeasonEntity animeSeasonEntity = animeSeasonEntityOptional.get();
            animeSeasonEntity.setStatus(false);
            animeSeasonRepository.saveAndFlush(animeSeasonEntity);
        }

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

    public AnimeDTO saveAnimeDTO(AnimeDTO animeDTO) throws RecordNotFoundException {
        // save anime
        AnimeEntity animeEntity = findById(animeDTO.getId());
        BeanKit.copyProperties(animeDTO, animeEntity);
        animeEntity = animeRepository.saveAndFlush(animeEntity);

        List<SeasonDTO> seasons = animeDTO.getSeasons();
        for (SeasonDTO season : seasons) {
            // update season
            Optional<SeasonEntity> seasonEntityOptional = seasonRepository.findById(season.getId());
            if (seasonEntityOptional.isEmpty()) {
                throw new RecordNotFoundException(
                    "season record not found, id=" + season.getId());
            }
            SeasonEntity seasonEntity = seasonEntityOptional.get();
            BeanKit.copyProperties(season, seasonEntity);
            season = seasonRepository.saveAndFlush(season);

            List<EpisodeDTO> episodes = season.getEpisodes();
            for (EpisodeDTO episode : episodes) {
                // update episode
                Optional<EpisodeEntity> episodeEntityOptional =
                    episodeRepository.findById(episode.getId());
                if (episodeEntityOptional.isEmpty()) {
                    throw new RecordNotFoundException(
                        "episode record not found, id=" + episode.getId());
                }
                EpisodeEntity episodeEntity = episodeEntityOptional.get();
                BeanKit.copyProperties(episode, episodeEntity);
                episodeRepository.saveAndFlush(episodeEntity);
            }
        }
        return animeDTO;
    }

    public AnimeDTO findAnimeDTOById(Long id) throws RecordNotFoundException {
        Assert.isPositive(id, "'id' must be positive");
        AnimeDTO animeDTO = new AnimeDTO();
        AnimeEntity animeEntity = findById(id);
        BeanKit.copyProperties(animeEntity, animeDTO);

        List<AnimeSeasonEntity> animeSeasonEntities =
            animeSeasonRepository.findByAnimeIdAndStatus(id, true);

        List<SeasonDTO> seasonDTOS = new ArrayList<>();
        for (AnimeSeasonEntity animeSeasonEntity : animeSeasonEntities) {
            Optional<SeasonEntity> seasonEntityOptional =
                seasonRepository.findById(animeSeasonEntity.getSeasonId());
            if (seasonEntityOptional.isEmpty()) {
                throw new RecordNotFoundException(
                    "season entity record not found: " + animeSeasonEntity.getSeasonId());
            }
            SeasonEntity seasonEntity = seasonEntityOptional.get();
            SeasonDTO seasonDTO = new SeasonDTO();
            BeanKit.copyProperties(seasonEntity, seasonDTO);

            List<SeasonEpisodeEntity> episodeEntityList =
                seasonEpisodeRepository.findBySeasonIdAndStatus(seasonEntity.getId(), true);
            List<EpisodeDTO> episodeDTOS = new ArrayList<>();
            for (SeasonEpisodeEntity seasonEpisodeEntity : episodeEntityList) {
                Long episodeId = seasonEpisodeEntity.getEpisodeId();

                Optional<EpisodeEntity> episodeEntityOptional =
                    episodeRepository.findById(episodeId);
                if (episodeEntityOptional.isEmpty()) {
                    throw new RecordNotFoundException(
                        "episode entity record not found: " + animeSeasonEntity.getSeasonId());
                }
                EpisodeEntity episodeEntity = episodeEntityOptional.get();
                EpisodeDTO episodeDTO = new EpisodeDTO();
                BeanKit.copyProperties(episodeEntity, episodeDTO);
                episodeDTOS.add(episodeDTO);
            }

            seasonDTO.setEpisodes(episodeDTOS);
            seasonDTOS.add(seasonDTO);
        }

        animeDTO.setSeasons(seasonDTOS);
        return animeDTO;
    }

    public PagingWrap<AnimeDTO> findAnimeDTOS(SearchAnimeDTOSParams searchAnimeDTOSParams) {
        Assert.notNull(searchAnimeDTOSParams, "'findAnimeDTOSParams' must not be null");
        Integer pageIndex = searchAnimeDTOSParams.getPage();
        Integer pageSize = searchAnimeDTOSParams.getSize();
        String title = searchAnimeDTOSParams.getTitle();
        String originalTitle = searchAnimeDTOSParams.getOriginalTitle();

        if (pageIndex == null) {
            Assert.isPositive(pageIndex, "'pageIndex' must be positive");
        }

        if (pageSize == null) {
            Assert.isPositive(pageSize, "'pageSize' must be positive");
        }

        List<AnimeEntity> animeEntities = null;

        // 构造自定义查询条件
        Specification<AnimeEntity> queryCondition = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            // 过滤掉逻辑删除的
            predicateList.add(criteriaBuilder.equal(root.get("status"), true));

            if (Strings.isNotBlank(title)) {
                predicateList.add(criteriaBuilder.like(root.get("title"), "%" + title + "%"));
            }
            if (Strings.isNotBlank(originalTitle)) {
                predicateList.add(criteriaBuilder.like(root.get("originalTitle"),
                    "%" + originalTitle + "%"));
            }

            Predicate[] predicates = new Predicate[predicateList.size()];
            return criteriaBuilder.and(predicateList.toArray(predicates));
        };

        // 分页和不分页，这里按起始页和每页展示条数为0时默认为不分页，分页的话按创建时间降序
        if (pageIndex == null || pageSize == null || (pageIndex == 0 && pageSize == 0)) {
            animeEntities = animeRepository.findAll(queryCondition);
        } else {
            // page小于1时，都为第一页, page从1开始，即第一页 pageIndex=1
            animeEntities = animeRepository.findAll(queryCondition,
                    PageRequest.of(pageIndex < 1 ? 0 : (pageIndex - 1), pageSize,
                        Sort.by(Sort.Direction.DESC, "createTime")))
                .getContent();
        }

        // find anime dto by id
        List<AnimeDTO> animeDTOList = new ArrayList<>(animeEntities.size());
        for (AnimeEntity animeEntity : animeEntities) {
            try {
                AnimeDTO animeDTO = findAnimeDTOById(animeEntity.getId());
                animeDTOList.add(animeDTO);
            } catch (RecordNotFoundException e) {
                LOGGER.warn("search anime id={} originalTitle={}   has exception: ",
                    animeEntity.getId(), animeEntity.getOriginalTitle(), e);
            }
        }

        return new PagingWrap<AnimeDTO>()
            .setContent(animeDTOList)
            .setCurrentIndex(pageIndex)
            .setTotal(animeRepository.count(queryCondition));

    }


    public AnimeDTO reqBgmtvBangumiMetadata(Long subjectId) throws RecordNotFoundException {
        try {
            // 直接返回已经存在的番剧数据
            List<AnimeEntity> animeEntityList =
                animeRepository.findByBgmtvIdAndStatus(subjectId, true);
            if (animeEntityList.isEmpty()) {
                throw new RecordNotFoundException("record not found, id=" + subjectId);
            }
            Long id = animeEntityList.get(0).getId();
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
            animeEntity
                .setBgmtvId(subjectId)
                .setCoverUrl(coverFileEntity.getUrl())
                .setAirTime(
                    DateKit.parseDateStr(bgmTvSubject.getDate(), BgmTvConstants.DATE_PATTERN))
                .setPlatform(bgmTvSubject.getPlatform())
                .setOverview(bgmTvSubject.getSummary())
                .setTitle(bgmTvSubject.getNameCn())
                .setOriginalTitle(bgmTvSubject.getName());

            if (Strings.isBlank(animeEntity.getTitle())) {
                animeEntity.setTitle(animeEntity.getOriginalTitle());
            }

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

            SeasonEntity seasonEntity = new SeasonEntity()
                .setType(SeasonEntity.Type.FIRST)
                .setOriginalTitle(animeEntity.getOriginalTitle())
                .setTitle(animeEntity.getTitle())
                .setOverview(animeEntity.getOverview());

            seasonEntity = seasonRepository.saveAndFlush(seasonEntity);

            animeSeasonRepository.saveAndFlush(new AnimeSeasonEntity()
                .setAnimeId(animeEntity.getId())
                .setSeasonId(seasonEntity.getId()));

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
                seasonEpisodeRepository.saveAndFlush(new SeasonEpisodeEntity()
                    .setSeasonId(seasonEntity.getId())
                    .setEpisodeId(episodeEntity.getId()));
            }

            return findAnimeDTOById(animeEntity.getId());
        }
    }

    public List<String> finSeasonTypes() {
        List<SeasonEntity.Type> types = Arrays.asList(SeasonEntity.Type.values());
        Collections.sort(types, new SeasonEntity.Type.OrderComparator());

        return types.stream()
            .flatMap((Function<SeasonEntity.Type, Stream<String>>) type
                -> Stream.of(type.name())).collect(Collectors.toList());
    }

}
