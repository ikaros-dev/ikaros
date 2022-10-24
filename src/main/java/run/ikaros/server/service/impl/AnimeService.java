package run.ikaros.server.service.impl;

import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.DateUtils;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.bgmtv.BgmTvConstants;
import run.ikaros.server.model.bgmtv.BgmTvEpisode;
import run.ikaros.server.model.bgmtv.BgmTvEpisodeType;
import run.ikaros.server.model.bgmtv.BgmTvSubject;
import run.ikaros.server.model.bgmtv.BgmTvTag;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.model.dto.EpisodeDTO;
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.params.SearchAnimeDTOSParams;
import run.ikaros.server.repository.AnimeRepository;
import run.ikaros.server.repository.EpisodeRepository;
import run.ikaros.server.repository.SeasonRepository;
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
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final UserServiceImpl userServiceImpl;
    private final BgmTvService bgmTvService;

    public AnimeService(AnimeRepository animeRepository,
                        SeasonRepository seasonRepository,
                        EpisodeRepository episodeRepository,
                        UserServiceImpl userServiceImpl, BgmTvService bgmTvService) {
        this.animeRepository = animeRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.userServiceImpl = userServiceImpl;
        this.bgmTvService = bgmTvService;
    }

    public AnimeEntity save(AnimeEntity animeEntity) {
        AssertUtils.notNull(animeEntity, "animeEntity");
        AssertUtils.notNull(animeEntity.getAirTime(), "airTime");
        AssertUtils.notBlank(animeEntity.getTitle(), "title");

        animeEntity = animeRepository.saveAndFlush(animeEntity);
        return animeEntity;
    }

    public SeasonEntity saveSeasonEntity(Long animeId, SeasonEntity seasonEntity) {
        AssertUtils.notNull(seasonEntity, "'seasonEntity' must not be null");
        AssertUtils.isPositive(animeId, "'animeId' must be positive");

        // save season
        String originalTitle = seasonEntity.getOriginalTitle();
        AssertUtils.notBlank(originalTitle, "'originalTitle' must not be blank");

        Optional<SeasonEntity> seasonEntityOptional =
            seasonRepository.findByIdAndTypeAndOriginalTitle(seasonEntity.getId(),
                seasonEntity.getType(),
                seasonEntity.getOriginalTitle());
        if (seasonEntityOptional.isPresent()) {
            SeasonEntity existSeasonEntity = seasonEntityOptional.get();
            BeanUtils.copyProperties(seasonEntity, existSeasonEntity);
            seasonEntity = existSeasonEntity;
        }
        seasonEntity = seasonRepository.saveAndFlush(seasonEntity);


        return seasonEntity;
    }


    public void removeAnimeSeason(Long animeId, Long seasonId) {
        AssertUtils.isPositive(animeId, "'animeId' must be positive");
        AssertUtils.isPositive(seasonId, "'seasonId' must be positive");

        // update season entity
        Optional<SeasonEntity> seasonEntityOptional = seasonRepository.findById(seasonId);
        if (seasonEntityOptional.isPresent()) {
            SeasonEntity seasonEntity = seasonEntityOptional.get();
            if (seasonEntity.getStatus()) {
                seasonEntity.setStatus(false);
                seasonRepository.saveAndFlush(seasonEntity);
            }
        }


    }


    public boolean existById(Long id) {
        AssertUtils.isPositive(id, "'id' must be positive");
        return animeRepository.existsByIdAndStatus(id, true);
    }

    public AnimeEntity findById(Long id) throws RecordNotFoundException {
        AssertUtils.isPositive(id, "'id' must be positive");
        if (!existById(id)) {
            throw new RecordNotFoundException("anime entity not exist, id=" + id);
        }
        return animeRepository.findByIdAndStatus(id, true).get();
    }

    public AnimeDTO saveAnimeDTO(AnimeDTO animeDTO) throws RecordNotFoundException {
        // save anime
        AnimeEntity animeEntity = findById(animeDTO.getId());
        BeanUtils.copyProperties(animeDTO, animeEntity);
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
            BeanUtils.copyProperties(season, seasonEntity);
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
                BeanUtils.copyProperties(episode, episodeEntity);
                episodeRepository.saveAndFlush(episodeEntity);
            }
        }
        return animeDTO;
    }

    public AnimeDTO findAnimeDTOById(Long id) throws RecordNotFoundException {
        AssertUtils.isPositive(id, "'id' must be positive");
        AnimeDTO animeDTO = new AnimeDTO();
        AnimeEntity animeEntity = findById(id);
        BeanUtils.copyProperties(animeEntity, animeDTO);

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
            BeanUtils.copyProperties(seasonEntity, seasonDTO);

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
                BeanUtils.copyProperties(episodeEntity, episodeDTO);
                episodeDTOS.add(episodeDTO);
            }

            seasonDTO.setEpisodes(episodeDTOS);
            seasonDTOS.add(seasonDTO);
        }

        animeDTO.setSeasons(seasonDTOS);
        return animeDTO;
    }

    public PagingWrap<AnimeDTO> findAnimeDTOS(SearchAnimeDTOSParams searchAnimeDTOSParams) {
        AssertUtils.notNull(searchAnimeDTOSParams, "'findAnimeDTOSParams' must not be null");
        Integer pageIndex = searchAnimeDTOSParams.getPage();
        Integer pageSize = searchAnimeDTOSParams.getSize();
        String title = searchAnimeDTOSParams.getTitle();
        String originalTitle = searchAnimeDTOSParams.getOriginalTitle();

        if (pageIndex == null) {
            AssertUtils.isPositive(pageIndex, "'pageIndex' must be positive");
        }

        if (pageSize == null) {
            AssertUtils.isPositive(pageSize, "'pageSize' must be positive");
        }

        List<AnimeEntity> animeEntities = null;

        // 构造自定义查询条件
        Specification<AnimeEntity> queryCondition = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            // 过滤掉逻辑删除的
            predicateList.add(criteriaBuilder.equal(root.get("status"), true));

            if (StringUtils.isNotBlank(title)) {
                predicateList.add(criteriaBuilder.like(root.get("title"), "%" + title + "%"));
            }
            if (StringUtils.isNotBlank(originalTitle)) {
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
                    DateUtils.parseDateStr(bgmTvSubject.getDate(), BgmTvConstants.DATE_PATTERN))
                .setPlatform(bgmTvSubject.getPlatform())
                .setOverview(bgmTvSubject.getSummary())
                .setTitle(bgmTvSubject.getNameCn())
                .setOriginalTitle(bgmTvSubject.getName());

            if (StringUtils.isBlank(animeEntity.getTitle())) {
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
                .setType(SeasonType.FIRST)
                .setOriginalTitle(animeEntity.getOriginalTitle())
                .setTitle(animeEntity.getTitle())
                .setOverview(animeEntity.getOverview());

            seasonEntity = seasonRepository.saveAndFlush(seasonEntity);

            animeSeasonRepository.saveAndFlush(new AnimeSeasonEntity()
                .setAnimeId(animeEntity.getId())
                .setSeasonId(seasonEntity.getId()));

            for (BgmTvEpisode bgmTvEpisode : bgmTvEpisodes) {
                Date airDate =
                    DateUtils.parseDateStr(bgmTvEpisode.getAirDate(), BgmTvConstants.DATE_PATTERN);
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

    public EpisodeEntity saveEpisodeEntity(Long seasonId, EpisodeEntity episodeEntity) {
        AssertUtils.isPositive(seasonId, "'seasonId' must be positive");
        AssertUtils.notNull(episodeEntity, "'episodeEntity' must not be null");
        Long episodeId = episodeEntity.getId();

        if (episodeId == null) {
            // 给季度新增剧集
            episodeEntity = episodeRepository.saveAndFlush(episodeEntity);
            seasonEpisodeRepository
                .saveAndFlush(new SeasonEpisodeEntity()
                    .setSeasonId(seasonId)
                    .setEpisodeId(episodeEntity.getId()));
        } else {
            // 更新剧集信息
            Optional<SeasonEpisodeEntity> seasonEpisodeEntityOptional =
                seasonEpisodeRepository.findBySeasonIdAndEpisodeId(seasonId, episodeId);
            if (seasonEpisodeEntityOptional.isPresent()
                && !seasonEpisodeEntityOptional.get().getStatus()) {
                SeasonEpisodeEntity seasonEpisodeEntity = seasonEpisodeEntityOptional.get();
                seasonEpisodeEntity.setStatus(true);
                seasonEpisodeRepository.saveAndFlush(seasonEpisodeEntity);
            }

            Optional<EpisodeEntity> episodeEntityOptional =
                episodeRepository.findBySeq(episodeEntity.getSeq());
            if (episodeEntityOptional.isPresent()) {
                EpisodeEntity existEpisodeEntity = episodeEntityOptional.get();
                BeanUtils.copyProperties(episodeEntity, existEpisodeEntity);
                episodeEntity = existEpisodeEntity;
            }
            episodeEntity = episodeRepository.saveAndFlush(episodeEntity);
        }

        return episodeEntity;
    }

    public void removeSeasonEpisode(Long seasonId, Long episodeId) {
        AssertUtils.isPositive(seasonId, "'seasonId' must be positive");
        AssertUtils.isPositive(episodeId, "'episodeId' must be positive");

        Optional<EpisodeEntity> episodeEntityOptional = episodeRepository.findById(episodeId);
        if (episodeEntityOptional.isPresent() && episodeEntityOptional.get().getStatus()) {
            EpisodeEntity episodeEntity = episodeEntityOptional.get();
            episodeEntity.setStatus(false);
            episodeRepository.saveAndFlush(episodeEntity);
        }

//        Optional<SeasonEpisodeEntity> seasonEpisodeEntityOptional =
//            seasonEpisodeRepository.findBySeasonIdAndEpisodeId(seasonId, episodeId);
//        if (seasonEpisodeEntityOptional.isPresent()
//            && seasonEpisodeEntityOptional.get().getStatus()) {
//            SeasonEpisodeEntity seasonEpisodeEntity = seasonEpisodeEntityOptional.get();
//            seasonEpisodeEntity.setStatus(false);
//            seasonEpisodeRepository.saveAndFlush(seasonEpisodeEntity);
//        }

    }
}
