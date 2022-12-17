package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.AnimeRepository;
import run.ikaros.server.core.repository.UserSubscribeRepository;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.core.service.UserSubscribeService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.BaseEntity;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.entity.UserSubscribeEntity;
import run.ikaros.server.enums.SubscribeType;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.model.dto.EpisodeDTO;
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.params.SearchAnimeDTOSParams;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.StringUtils;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class AnimeServiceImpl
    extends AbstractCrudService<AnimeEntity, Long>
    implements AnimeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeServiceImpl.class);

    private final AnimeRepository animeRepository;
    private final UserSubscribeRepository userSubscribeRepository;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;
    private final FileService fileService;

    public AnimeServiceImpl(AnimeRepository animeRepository,
                            SeasonService seasonService,
                            EpisodeService episodeService, FileService fileService,
                            UserSubscribeRepository userSubscribeRepository) {
        super(animeRepository);
        this.animeRepository = animeRepository;
        this.seasonService = seasonService;
        this.episodeService = episodeService;
        this.fileService = fileService;
        this.userSubscribeRepository = userSubscribeRepository;
    }

    @Nonnull
    public AnimeEntity save(@Nonnull AnimeEntity animeEntity) {
        AssertUtils.notNull(animeEntity, "animeEntity");
        AssertUtils.notBlank(animeEntity.getTitle(), "title");

        animeEntity = animeRepository.saveAndFlush(animeEntity);
        return animeEntity;
    }

    public AnimeEntity findById(Long id) {
        AssertUtils.isPositive(id, "'id' must be positive");
        return animeRepository
            .findByIdAndStatus(id, true)
            .orElseThrow(() -> new RecordNotFoundException("anime not exist, id=" + id));
    }

    @Nonnull
    @Override
    public AnimeDTO saveAnimeDTO(@Nonnull AnimeDTO animeDTO) {
        AssertUtils.notNull(animeDTO, "animeDTO");
        // save anime
        AnimeEntity animeEntity = findById(animeDTO.getId());
        BeanUtils.copyProperties(animeDTO, animeEntity);
        animeEntity = animeRepository.saveAndFlush(animeEntity);
        BeanUtils.copyProperties(animeEntity, animeDTO);

        for (SeasonDTO seasonDTO : animeDTO.getSeasons()) {
            // update season
            AssertUtils.isPositive(seasonDTO.getAnimeId(), "animeId");
            SeasonEntity seasonEntity = new SeasonEntity();
            BeanUtils.copyProperties(seasonDTO, seasonEntity);
            seasonEntity = seasonService.save(seasonEntity);
            BeanUtils.copyProperties(seasonEntity, seasonDTO);

            // update episode
            for (EpisodeDTO episodeDTO : seasonDTO.getEpisodes()) {
                EpisodeEntity episodeEntity = new EpisodeEntity();
                BeanUtils.copyProperties(episodeDTO, episodeEntity);
                AssertUtils.isPositive(episodeEntity.getSeasonId(), "seasonId");
                episodeEntity = episodeService.save(episodeEntity);
                BeanUtils.copyProperties(episodeEntity, episodeDTO);
            }
        }
        return animeDTO;
    }

    @Nonnull
    public AnimeDTO findAnimeDTOById(@Nonnull Long id) {
        AssertUtils.isPositive(id, "'id' must be positive");
        AnimeDTO animeDTO = new AnimeDTO();
        AnimeEntity animeEntity = findById(id);
        BeanUtils.copyProperties(animeEntity, animeDTO);
        animeDTO.setSeasons(seasonService.findDtoListByAnimeId(id));

        // search current user subscribe status
        Optional<UserSubscribeEntity> userSubscribeEntityOptional =
            userSubscribeRepository
                .findByUserIdAndTypeAndTargetId(UserService.getCurrentLoginUserUid(),
                    SubscribeType.ANIME, animeDTO.getId());
        boolean isSub = userSubscribeEntityOptional.isPresent()
            && userSubscribeEntityOptional.get().getStatus();
        animeDTO.setSub(isSub);
        if (isSub) {
            animeDTO.setSubscribe(userSubscribeEntityOptional.orElse(null));
        }
        return animeDTO;
    }

    @Nonnull
    @Override
    public PagingWrap<AnimeDTO> findAnimeDTOS(
        @Nonnull SearchAnimeDTOSParams searchAnimeDTOSParams) {
        AssertUtils.notNull(searchAnimeDTOSParams, "'findAnimeDTOSParams' must not be null");
        Integer pageIndex = searchAnimeDTOSParams.getPage();
        Integer pageSize = searchAnimeDTOSParams.getSize();
        String title = searchAnimeDTOSParams.getTitle();
        String titleCn = searchAnimeDTOSParams.getTitleCn();

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
            if (StringUtils.isNotBlank(titleCn)) {
                predicateList.add(criteriaBuilder.like(root.get("titleCn"),
                    "%" + titleCn + "%"));
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
                LOGGER.warn("search anime id={} title={}   has exception: ",
                    animeEntity.getId(), animeEntity.getTitle(), e);
            }
        }

        return new PagingWrap<AnimeDTO>()
            .setContent(animeDTOList)
            .setCurrentIndex(pageIndex)
            .setTotal(animeRepository.count(queryCondition));

    }

    @Override
    public AnimeEntity findByBgmTvId(@Nonnull Long bgmtvId) {
        AssertUtils.notNull(bgmtvId, "bgmtvId");
        List<AnimeEntity> animeEntityList =
            animeRepository.findByBgmtvIdAndStatus(bgmtvId, true);
        if (animeEntityList == null || animeEntityList.isEmpty()) {
            return null;
        } else {
            return animeEntityList.get(0);
        }
    }

    @Nonnull
    @Override
    public Optional<AnimeEntity> findByTitle(@Nonnull String title) {
        AssertUtils.notBlank(title, "title");
        return animeRepository.findByTitleAndStatus(title, true);
    }

    @Nonnull
    @Override
    public Optional<AnimeEntity> findByTitleCn(@Nonnull String titleCn) {
        AssertUtils.notBlank(titleCn, "titleCn");
        return animeRepository.findByTitleCnAndStatus(titleCn, true);
    }

    @Nonnull
    @Override
    public List<AnimeEntity> findByTitleLike(@Nonnull String title) {
        AssertUtils.notBlank(title, "title");
        return animeRepository.findByStatusAndTitleLike(true, StringUtils.addLikeChar(title));
    }

    @Nonnull
    @Override
    public List<AnimeEntity> findByTitleCnLike(@Nonnull String titleCn) {
        AssertUtils.notBlank(titleCn, "titleCn");
        return animeRepository.findByStatusAndTitleCnLike(true, StringUtils.addLikeChar(titleCn));
    }

    @Override
    public AnimeEntity deleteByIdLogically(@Nonnull Long animeId) {
        AssertUtils.isPositive(animeId, "anime id");
        AnimeEntity animeEntity = getById(animeId);
        animeEntity.setStatus(false);
        return save(animeEntity);
    }

    @Nonnull
    @Override
    public List<AnimeEntity> findAll() {
        return listAll()
            .stream()
            .filter(BaseEntity::getStatus)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteWithBatchLogicallyByIds(@Nonnull Long[] animeIdArray) {
        AssertUtils.notNull(animeIdArray, "anime id array");
        for (Long animeId : animeIdArray) {
            deleteByIdLogically(animeId);
        }
    }


}
