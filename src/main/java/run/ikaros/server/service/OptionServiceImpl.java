package run.ikaros.server.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.DefaultConst;
import run.ikaros.server.constants.OptionConst;
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
import run.ikaros.server.exceptions.ReflectOperateException;
import run.ikaros.server.init.option.AppPresetOption;
import run.ikaros.server.init.option.PresetOption;
import run.ikaros.server.core.repository.OptionRepository;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.model.request.AppInitRequest;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.ClassUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.StringUtils;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Service
public class OptionServiceImpl
    extends AbstractCrudService<OptionEntity, Long>
    implements OptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionServiceImpl.class);

    private List<Class<? extends PresetOption>> classList = null;

    private final OptionRepository optionRepository;
    private final UserService userService;

    public OptionServiceImpl(OptionRepository optionRepository, UserService userService) {
        super(optionRepository);
        this.optionRepository = optionRepository;
        this.userService = userService;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<Class<? extends PresetOption>> getPresetOptionClassList() {
        try {
            if (classList != null) {
                return classList;
            }

            classList =
                ClassUtils
                    .findClassByPackage(OptionConst.INIT_PRESET_OPTION_PACKAGE_NAME)
                    .stream()
                    .filter(cls -> Arrays.stream(cls.getInterfaces())
                        .collect(Collectors.toSet())
                        .contains(PresetOption.class))
                    .flatMap((Function<Class<?>, Stream<Class<? extends PresetOption>>>) cls
                        -> Stream.of((Class<? extends PresetOption>) cls))
                    .toList();

            return classList;
        } catch (IOException e) {
            throw new ReflectOperateException(e);
        }
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

    @Nonnull
    @Override
    public <T extends PresetOption> T findPresetOption(@Nonnull T presetOption) {
        OptionCategory category = presetOption.getCategory();
        List<OptionEntity> optionEntityList =
            optionRepository.findByCategoryAndStatus(category, true);

        for (Field field : presetOption.getClass().getDeclaredFields()) {
            for (OptionEntity optionEntity : optionEntityList) {
                if (field.getName().equalsIgnoreCase(optionEntity.getKey())) {
                    field.setAccessible(true);
                    try {
                        field.set(presetOption, optionEntity.getValue());
                    } catch (IllegalAccessException e) {
                        throw new ReflectOperateException(e);
                    }
                }
            }
        }

        return presetOption;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public List<PresetOption> findPresetOptionList() {
        List<PresetOption> presetOptionList = new ArrayList<>();
        try {
            List<Class<? extends PresetOption>> classList = getPresetOptionClassList();

            for (Class<? extends PresetOption> cls : classList) {
                PresetOption presetOption = cls.newInstance();
                presetOptionList.add(findPresetOption(presetOption));
            }

        } catch (ReflectiveOperationException e) {
            throw new ReflectOperateException(e);
        }

        return presetOptionList;
    }

    @Override
    public <T extends PresetOption> T savePresetOption(@Nonnull T presetOption) {
        AssertUtils.notNull(presetOption, "presetOption");
        OptionCategory category = presetOption.getCategory();
        List<OptionEntity> optionEntityList = findOptionByCategory(category);

        // update data from preset to database
        for (OptionEntity optionEntity : optionEntityList) {
            for (Field field : presetOption.getClass().getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(optionEntity.getKey())) {
                    field.setAccessible(true);
                    String newValue;
                    try {
                        newValue = (String) field.get(presetOption);
                    } catch (IllegalAccessException e) {
                        throw new ReflectOperateException(e);
                    }
                    if (newValue != null) {
                        optionEntity.setValue(newValue);
                        optionEntity = optionRepository.save(optionEntity);
                    }
                }
            }
        }

        // flush all update to database
        optionRepository.flush();

        // find preset form database to current preset
        return findPresetOption(presetOption);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void initPresetOptionsOnce() {
        // search has init
        AppPresetOption appPresetOption =
            findPresetOption(new AppPresetOption().setIsInit("false"));
        if (appPresetOption.getIsInit().equalsIgnoreCase("true")) {
            return;
        }

        // read preset package all PresetOption
        List<Class<? extends PresetOption>> classList = getPresetOptionClassList();

        // build option entity list by all preset option
        List<OptionEntity> optionEntityList = new ArrayList<>();
        for (Class<?> cls : classList) {
            PresetOption presetOption = null;
            try {
                presetOption = (PresetOption) cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ReflectOperateException(e);
            }
            optionEntityList.addAll(PresetOption.buildEntityListByPresetOption(presetOption));
        }

        // save all option entity
        optionEntityList.forEach(this::save);
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

}
