package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.DefaultConst;
import run.ikaros.server.core.repository.OptionRepository;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionApp;
import run.ikaros.server.enums.OptionBgmTv;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionCommon;
import run.ikaros.server.enums.OptionFile;
import run.ikaros.server.enums.OptionJellyfin;
import run.ikaros.server.enums.OptionMikan;
import run.ikaros.server.enums.OptionNetwork;
import run.ikaros.server.enums.OptionQbittorrent;
import run.ikaros.server.enums.OptionSeo;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.OptionDTO;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.model.request.AppInitRequest;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Service
public class OptionServiceImpl
    extends AbstractCrudService<OptionEntity, Long>
    implements OptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionServiceImpl.class);

    private final OptionRepository optionRepository;
    private final UserService userService;

    public OptionServiceImpl(OptionRepository optionRepository, UserService userService) {
        super(optionRepository);
        this.optionRepository = optionRepository;
        this.userService = userService;
    }

    @Nonnull
    @Override
    public OptionEntity findOptionItemByKey(@Nonnull String key) {
        AssertUtils.notBlank(key, "'key' must not be blank");
        if (!optionRepository.existsByKeyAndStatus(key, true)) {
            throw new RecordNotFoundException("target key option record not fond, key=" + key);
        }
        return optionRepository.findByKeyAndStatus(key, true);
    }

    @Nonnull
    @Override
    public OptionEntity saveOptionItem(@Nonnull OptionItemDTO optionItemDTO) {
        AssertUtils.notNull(optionItemDTO, "'optionItemDTO' must not be null");
        String key = optionItemDTO.getKey();
        AssertUtils.notBlank(key, "'key' must not be blank");

        try {
            OptionEntity existOptionEntity = findOptionItemByKey(key);
            existOptionEntity.setValue(optionItemDTO.getValue())
                .setCategory(optionItemDTO.getCategory());
            return optionRepository.saveAndFlush(existOptionEntity);
        } catch (RecordNotFoundException e) {
            OptionEntity optionEntity
                = new OptionEntity()
                .setKey(key)
                .setValue(optionItemDTO.getValue())
                .setType(optionItemDTO.getType())
                .setCategory(optionItemDTO.getCategory());
            LOGGER.debug("create new option record: {}", JsonUtils.obj2Json(optionEntity));
            return optionRepository.saveAndFlush(optionEntity);
        }

    }

    @Override
    public void deleteOptionItemByKey(@Nonnull String key) {
        AssertUtils.notBlank(key, "'key' must not be blank");
        OptionEntity optionEntity = findOptionItemByKey(key);
        optionEntity.setStatus(false);
        optionRepository.saveAndFlush(optionEntity);
    }

    @Nonnull
    @Override
    public List<OptionEntity> findOptionByCategory(@Nonnull OptionCategory category) {
        AssertUtils.notNull(category, "category");
        return optionRepository.findByCategoryAndStatus(category, true);
    }

    @Nonnull
    @Override
    public OptionEntity findOptionValueByCategoryAndKey(@Nonnull OptionCategory category,
                                                        @Nonnull String key) {
        AssertUtils.notNull(category, "category");
        AssertUtils.notBlank(key, "key");
        OptionEntity optionEntity =
            optionRepository.findByCategoryAndKeyAndStatus(category, key, true);
        if (optionEntity == null) {
            throw new RecordNotFoundException("target option record not fond, key=" + key
                + " category=" + category);
        }
        return optionEntity;
    }

    @Override
    public boolean findAppIsInit() {
        OptionEntity optionEntity =
            optionRepository.findByCategoryAndKeyAndStatus(OptionCategory.APP,
                OptionApp.IS_INIT.name(), true);
        if (optionEntity != null) {
            return "true".equalsIgnoreCase(optionEntity.getValue());
        }
        return false;
    }

    @Override
    public boolean appInit(@Nonnull AppInitRequest appInitRequest) {
        if (findAppIsInit()) {
            return true;
        }

        AssertUtils.notNull(appInitRequest, "appInitRequest");

        final String username = appInitRequest.getUsername();
        final String password = appInitRequest.getPassword();
        AssertUtils.notBlank(username, "username");
        AssertUtils.notBlank(password, "password");
        userService.registerUserByUsernameAndPassword(username, password);

        final String title = appInitRequest.getTitle();
        final String description = appInitRequest.getDescription();

        // init option app
        saveOptionItem(new OptionItemDTO(OptionApp.IS_INIT.name(),
            DefaultConst.OPTION_APP_IS_INIT, OptionCategory.APP));
        saveOptionItem(new OptionItemDTO(OptionApp.THEME.name(),
            DefaultConst.OPTION_APP_THEME, OptionCategory.APP));

        // init option common
        saveOptionItem(new OptionItemDTO(OptionCommon.TITLE.name(),
            StringUtils.isNotBlank(title) ? title : DefaultConst.OPTION_COMMON_TITLE,
            OptionCategory.COMMON));
        saveOptionItem(new OptionItemDTO(OptionCommon.DESCRIPTION.name(),
            StringUtils.isNotBlank(description) ? description :
                DefaultConst.OPTION_COMMON_DESCRIPTION,
            OptionCategory.COMMON));
        saveOptionItem(new OptionItemDTO(OptionCommon.ADDRESS.name(),
            DefaultConst.OPTION_COMMON_ADDRESS,
            OptionCategory.COMMON));
        saveOptionItem(new OptionItemDTO(OptionCommon.LOGO.name(),
            DefaultConst.OPTION_COMMON_LOGO,
            OptionCategory.COMMON));
        saveOptionItem(new OptionItemDTO(OptionCommon.FAVICON.name(),
            DefaultConst.OPTION_COMMON_FAVICON,
            OptionCategory.COMMON));
        saveOptionItem(new OptionItemDTO(OptionCommon.HEADER.name(),
            DefaultConst.OPTION_COMMON_HEADER,
            OptionCategory.COMMON));
        saveOptionItem(new OptionItemDTO(OptionCommon.FOOTER.name(),
            DefaultConst.OPTION_COMMON_FOOTER,
            OptionCategory.COMMON));
        saveOptionItem(new OptionItemDTO(OptionCommon.STATISTICS_CODE.name(),
            DefaultConst.OPTION_COMMON_STATISTICS_CODE,
            OptionCategory.COMMON));

        // init option seo
        saveOptionItem(new OptionItemDTO(OptionSeo.HIDE_FOR_SEARCH_ENGINE.name(),
            DefaultConst.OPTION_SEO_HIDE_FOR_SEARCH_ENGINE, OptionCategory.SEO));
        saveOptionItem(new OptionItemDTO(OptionSeo.KEYWORDS.name(),
            DefaultConst.OPTION_SEO_KEYWORDS, OptionCategory.SEO));
        saveOptionItem(new OptionItemDTO(OptionSeo.SITE_DESCRIPTION.name(),
            DefaultConst.OPTION_SEO_SITE_DESCRIPTION, OptionCategory.SEO));

        // init option file
        saveOptionItem(new OptionItemDTO(OptionFile.PLACE_SELECT.name(),
            DefaultConst.OPTION_FILE_PLACE_SELECT, OptionCategory.FILE));

        // init option network
        saveOptionItem(new OptionItemDTO(OptionNetwork.PROXY_HTTP_HOST.name(),
            DefaultConst.OPTION_NETWORK_PROXY_HTTP_HOST, OptionCategory.NETWORK));
        saveOptionItem(new OptionItemDTO(OptionNetwork.PROXY_HTTP_PORT.name(),
            DefaultConst.OPTION_NETWORK_PROXY_HTTP_PORT, OptionCategory.NETWORK));

        // init option qbittorrent
        saveOptionItem(new OptionItemDTO(OptionQbittorrent.URL.name(),
            DefaultConst.OPTION_QBITTORRENT_URL, OptionCategory.QBITTORRENT));
        saveOptionItem(new OptionItemDTO(OptionQbittorrent.USERNAME.name(),
            DefaultConst.OPTION_QBITTORRENT_USERNAME, OptionCategory.QBITTORRENT));
        saveOptionItem(new OptionItemDTO(OptionQbittorrent.PASSWORD.name(),
            DefaultConst.OPTION_QBITTORRENT_PASSWORD, OptionCategory.QBITTORRENT));

        // init option bgmtv
        saveOptionItem(new OptionItemDTO(OptionBgmTv.API_BASE.name(),
            DefaultConst.OPTION_BGMTV_API_BASE, OptionCategory.BGMTV));
        saveOptionItem(new OptionItemDTO(OptionBgmTv.API_SUBJECTS.name(),
            DefaultConst.OPTION_BGMTV_API_SUBJECTS, OptionCategory.BGMTV));
        saveOptionItem(new OptionItemDTO(OptionBgmTv.API_EPISODES.name(),
            DefaultConst.OPTION_BGMTV_API_EPISODES, OptionCategory.BGMTV));
        saveOptionItem(new OptionItemDTO(OptionBgmTv.API_SEARCH_SUBJECT.name(),
            DefaultConst.OPTION_BGMTV_API_SEARCH_SUBJECT, OptionCategory.BGMTV));
        saveOptionItem(new OptionItemDTO(OptionBgmTv.ENABLE_PROXY.name(),
            DefaultConst.OPTION_BGMTV_ENABLE_PROXY, OptionCategory.BGMTV));

        // init option mikan
        saveOptionItem(new OptionItemDTO(OptionMikan.MY_SUBSCRIBE_RSS.name(),
            DefaultConst.OPTION_MIKAN_MY_SUBSCRIBE_RSS, OptionCategory.MIKAN));
        saveOptionItem(new OptionItemDTO(OptionMikan.ENABLE_PROXY.name(),
            DefaultConst.OPTION_MIKAN_ENABLE_PROXY, OptionCategory.MIKAN));

        // init option jellyfin
        saveOptionItem(new OptionItemDTO(OptionJellyfin.MEDIA_DIR_PATH.name(),
            DefaultConst.OPTION_JELLYFIN_MEDIA_DIR_PATH, OptionCategory.JELLYFIN));

        return true;
    }

    @Nonnull
    @Override
    public List<OptionDTO> findOptions(@Nullable String category) {
        List<OptionEntity> optionEntityList = new ArrayList<>();
        if (StringUtils.isNotBlank(category)) {
            category = category.toUpperCase(Locale.ROOT);
            if (!OptionCategory.CATEGORY_SET.contains(category)) {
                throw new IllegalArgumentException("please input correct category name from: "
                    + JsonUtils.obj2Json(OptionCategory.CATEGORY_SET));
            }
            OptionEntity optionEntityExample
                = new OptionEntity()
                .setCategory(OptionCategory.valueOf(category))
                .setKey(null)
                .setValue(null);
            optionEntityList.addAll(listAll(Example.of(optionEntityExample)));
        } else {
            optionEntityList.addAll(listAll());
        }
        return optionEntityList
            .stream()
            .flatMap((Function<OptionEntity, Stream<OptionDTO>>) optionEntity -> {
                OptionDTO optionDTO = new OptionDTO();
                optionDTO.setCategory(optionEntity.getCategory().name());
                optionDTO.setKey(optionEntity.getKey());
                optionDTO.setValue(optionEntity.getValue());
                return Stream.of(optionDTO);
            }).collect(Collectors.toList());
    }

}
