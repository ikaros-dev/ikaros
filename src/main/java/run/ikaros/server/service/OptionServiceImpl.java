package run.ikaros.server.service;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Example;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.DefaultConst;
import run.ikaros.server.core.repository.OptionRepository;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.MailProtocol;
import run.ikaros.server.enums.OptionApp;
import run.ikaros.server.enums.OptionBgmTv;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionCommon;
import run.ikaros.server.enums.OptionFile;
import run.ikaros.server.enums.OptionJellyfin;
import run.ikaros.server.enums.OptionMikan;
import run.ikaros.server.enums.OptionNetwork;
import run.ikaros.server.enums.OptionNotify;
import run.ikaros.server.enums.OptionQbittorrent;
import run.ikaros.server.enums.OptionSeo;
import run.ikaros.server.event.BgmTvHttpProxyUpdateEvent;
import run.ikaros.server.event.BgmTvTokenUpdateEvent;
import run.ikaros.server.event.MikanAndRssHttpProxyUpdateEvent;
import run.ikaros.server.event.OptionNetworkUpdateEvent;
import run.ikaros.server.event.OptionNotifyUpdateEvent;
import run.ikaros.server.event.QbittorrentOptionUpdateEvent;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.OptionBgmTvDTO;
import run.ikaros.server.model.dto.OptionDTO;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.model.dto.OptionNetworkDTO;
import run.ikaros.server.model.dto.OptionNotifyDTO;
import run.ikaros.server.model.dto.OptionQbittorrentDTO;
import run.ikaros.server.model.request.AppInitRequest;
import run.ikaros.server.model.request.SaveOptionRequest;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Slf4j
@Service
public class OptionServiceImpl
    extends AbstractCrudService<OptionEntity, Long>
    implements OptionService, ApplicationContextAware {
    private final OptionRepository optionRepository;
    private final UserService userService;
    private ApplicationContext applicationContext;

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

        OptionEntity existOptionEntity =
            findOptionValueByCategoryAndKey(optionItemDTO.getCategory(), key);
        if (existOptionEntity == null) {
            OptionEntity optionEntity
                = new OptionEntity()
                .setKey(key)
                .setValue(optionItemDTO.getValue())
                .setType(optionItemDTO.getType())
                .setCategory(optionItemDTO.getCategory());
            log.debug("create new option record: {}", JsonUtils.obj2Json(optionEntity));
            return optionRepository.saveAndFlush(optionEntity);
        } else {
            existOptionEntity.setValue(optionItemDTO.getValue())
                .setCategory(optionItemDTO.getCategory());
            return optionRepository.saveAndFlush(existOptionEntity);
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

    @Nullable
    @Override
    public OptionEntity findOptionValueByCategoryAndKey(@Nonnull OptionCategory category,
                                                        @Nonnull String key) {
        AssertUtils.notNull(category, "category");
        AssertUtils.notBlank(key, "key");
        return optionRepository.findByCategoryAndKeyAndStatus(category, key, true);
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
        return appInit(appInitRequest, false);
    }

    @Override
    public boolean appInit(@Nonnull AppInitRequest appInitRequest, boolean isCoerce) {
        if (!isCoerce && findAppIsInit()) {
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
        saveOptionItem(new OptionItemDTO(OptionApp.ENABLE_AUTO_ANIME_SUB_TASK.name(),
            DefaultConst.OPTION_APP_ENABLE_AUTO_ANIME_SUB_TASK, OptionCategory.APP));
        saveOptionItem(new OptionItemDTO(OptionApp.ENABLE_GENERATE_MEDIA_DIR_TASK.name(),
            DefaultConst.OPTION_APP_ENABLE_GENERATE_MEDIA_DIR_TASK, OptionCategory.APP));

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
        saveOptionItem(new OptionItemDTO(OptionNetwork.CONNECT_TIMEOUT.name(),
            DefaultConst.OPTION_NETWORK_CONNECT_TIMEOUT, OptionCategory.NETWORK));
        saveOptionItem(new OptionItemDTO(OptionNetwork.READ_TIMEOUT.name(),
            DefaultConst.OPTION_NETWORK_READ_TIMEOUT, OptionCategory.NETWORK));

        // init option qbittorrent
        saveOptionItem(new OptionItemDTO(OptionQbittorrent.URL.name(),
            DefaultConst.OPTION_QBITTORRENT_URL, OptionCategory.QBITTORRENT));
        saveOptionItem(new OptionItemDTO(OptionQbittorrent.ENABLE_AUTH.name(),
            DefaultConst.OPTION_QBITTORRENT_ENABLE_AUTH, OptionCategory.QBITTORRENT));
        saveOptionItem(new OptionItemDTO(OptionQbittorrent.USERNAME.name(),
            DefaultConst.OPTION_QBITTORRENT_USERNAME, OptionCategory.QBITTORRENT));
        saveOptionItem(new OptionItemDTO(OptionQbittorrent.PASSWORD.name(),
            DefaultConst.OPTION_QBITTORRENT_PASSWORD, OptionCategory.QBITTORRENT));

        // init option bgmtv
        saveOptionItem(new OptionItemDTO(OptionBgmTv.ENABLE_PROXY.name(),
            DefaultConst.OPTION_BGMTV_ENABLE_PROXY, OptionCategory.BGMTV));
        saveOptionItem(new OptionItemDTO(OptionBgmTv.ACCESS_TOKEN.name(),
            DefaultConst.OPTION_BGMTV_ACCESS_TOKEN, OptionCategory.BGMTV));

        // init option mikan
        saveOptionItem(new OptionItemDTO(OptionMikan.MY_SUBSCRIBE_RSS.name(),
            DefaultConst.OPTION_MIKAN_MY_SUBSCRIBE_RSS, OptionCategory.MIKAN));
        saveOptionItem(new OptionItemDTO(OptionMikan.ENABLE_PROXY.name(),
            DefaultConst.OPTION_MIKAN_ENABLE_PROXY, OptionCategory.MIKAN));

        // init option jellyfin
        saveOptionItem(new OptionItemDTO(OptionJellyfin.MEDIA_DIR_PATH.name(),
            DefaultConst.OPTION_JELLYFIN_MEDIA_DIR_PATH, OptionCategory.JELLYFIN));

        // init option notify
        saveOptionItem(new OptionItemDTO(OptionNotify.MAIL_ENABLE.name(),
            DefaultConst.OPTION_NOTIFY_MAIL_ENABLE, OptionCategory.NOTIFY));
        saveOptionItem(new OptionItemDTO(OptionNotify.MAIL_PROTOCOL.name(),
            DefaultConst.OPTION_NOTIFY_MAIL_PROTOCOL, OptionCategory.NOTIFY));
        saveOptionItem(new OptionItemDTO(OptionNotify.MAIL_SMTP_HOST.name(),
            DefaultConst.OPTION_NOTIFY_MAIL_SMTP_HOST, OptionCategory.NOTIFY));
        saveOptionItem(new OptionItemDTO(OptionNotify.MAIL_SMTP_PORT.name(),
            DefaultConst.OPTION_NOTIFY_MAIL_SMTP_PORT, OptionCategory.NOTIFY));
        saveOptionItem(new OptionItemDTO(OptionNotify.MAIL_SMTP_ACCOUNT.name(),
            DefaultConst.OPTION_NOTIFY_MAIL_SMTP_ACCOUNT, OptionCategory.NOTIFY));
        saveOptionItem(new OptionItemDTO(OptionNotify.MAIL_SMTP_PASSWORD.name(),
            DefaultConst.OPTION_NOTIFY_MAIL_SMTP_PASSWORD, OptionCategory.NOTIFY));
        saveOptionItem(new OptionItemDTO(OptionNotify.MAIL_SMTP_ACCOUNT_ALIAS.name(),
            DefaultConst.OPTION_NOTIFY_MAIL_SMTP_ACCOUNT_ALIAS, OptionCategory.NOTIFY));

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

    @Nonnull
    @Override
    public List<OptionDTO> saveWithRequest(@Nonnull SaveOptionRequest saveOptionRequest) {
        AssertUtils.notNull(saveOptionRequest, "saveOptionRequest");
        List<OptionDTO> optionDTOList = new ArrayList<>();
        String category = saveOptionRequest.getCategory();
        Map<String, String> kvMap = saveOptionRequest.getKvMap();
        if (!OptionCategory.CATEGORY_SET.contains(category)) {
            throw new IllegalArgumentException("please input correct category name from: "
                + JsonUtils.obj2Json(OptionCategory.CATEGORY_SET));
        }
        OptionCategory optionCategory = OptionCategory.valueOf(category);
        for (Map.Entry<String, String> entry : kvMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 需要针对特殊配置项预留较为通用的校验钩子
            if (OptionCategory.APP.name().equalsIgnoreCase(category)
                && OptionApp.ENABLE_AUTO_ANIME_SUB_TASK.name().equalsIgnoreCase(key)) {
                if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
                    checkAppEnableAutoAnimeSubTaskRunEnv();
                }
            }

            if (OptionCategory.MIKAN.name().equalsIgnoreCase(category)
                && OptionMikan.ENABLE_PROXY.name().equalsIgnoreCase(key)) {
                if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
                    checkNetworkHttpProxyHasConfig();
                }
                OptionNetworkDTO optionNetworkDTO = getOptionNetworkDTO();
                MikanAndRssHttpProxyUpdateEvent mikanAndRssHttpProxyUpdateEvent =
                    new MikanAndRssHttpProxyUpdateEvent(this,
                        Boolean.valueOf(value),
                        optionNetworkDTO.getProxyHttpHost(),
                        optionNetworkDTO.getProxyHttpPort(),
                        optionNetworkDTO.getReadTimeout(),
                        optionNetworkDTO.getConnectTimeout());
                applicationContext.publishEvent(mikanAndRssHttpProxyUpdateEvent);
                log.debug("publish MikanAndRssHttpProxyUpdateEvent");
            }

            if (OptionCategory.BGMTV.name().equalsIgnoreCase(category)
                && OptionBgmTv.ENABLE_PROXY.name().equalsIgnoreCase(key)) {
                if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
                    checkNetworkHttpProxyHasConfig();
                }
                OptionNetworkDTO optionNetworkDTO = getOptionNetworkDTO();
                BgmTvHttpProxyUpdateEvent bgmTvHttpProxyUpdateEvent =
                    new BgmTvHttpProxyUpdateEvent(this, Boolean.valueOf(value),
                        optionNetworkDTO.getProxyHttpHost(),
                        optionNetworkDTO.getProxyHttpPort(),
                        optionNetworkDTO.getReadTimeout(),
                        optionNetworkDTO.getConnectTimeout());
                log.debug("publish BgmTvHttpProxyUpdateEvent with enable={}", value);
                applicationContext.publishEvent(bgmTvHttpProxyUpdateEvent);
                log.debug("publish EpisodeUrlUpdateEvent");
            }

            if (OptionCategory.BGMTV.name().equalsIgnoreCase(category)
                && OptionBgmTv.ACCESS_TOKEN.toString().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                BgmTvTokenUpdateEvent bgmTvTokenUpdateEvent =
                    new BgmTvTokenUpdateEvent(this, value);
                applicationContext.publishEvent(bgmTvTokenUpdateEvent);
                log.debug("publish BgmTvTokenUpdateEvent");
            }

            if (OptionCategory.NETWORK.name().equalsIgnoreCase(category)) {
                String enableBgmTvHttpProxy = "false";
                String enableMikanHttpProxy = "false";
                try {
                    OptionEntity enableBgmtvHttpProxyOptionEntity =
                        findOptionValueByCategoryAndKey(OptionCategory.BGMTV,
                            OptionBgmTv.ENABLE_PROXY.name());
                    if (enableBgmtvHttpProxyOptionEntity != null
                        && Boolean.TRUE.toString()
                        .equalsIgnoreCase(enableBgmtvHttpProxyOptionEntity.getValue())) {
                        enableBgmTvHttpProxy = enableBgmtvHttpProxyOptionEntity.getValue();
                    }
                    OptionEntity enableMikanHttpProxyOptionEntity =
                        findOptionValueByCategoryAndKey(OptionCategory.MIKAN,
                            OptionMikan.ENABLE_PROXY.name());
                    if (enableMikanHttpProxyOptionEntity != null
                        && Boolean.TRUE.toString()
                        .equalsIgnoreCase(enableMikanHttpProxyOptionEntity.getValue())) {
                        enableMikanHttpProxy = enableMikanHttpProxyOptionEntity.getValue();
                    }
                } catch (RecordNotFoundException recordNotFoundException) {
                    // app not init, default is false
                }

                if (OptionNetwork.PROXY_HTTP_HOST.name().equalsIgnoreCase(key)) {
                    OptionNetworkDTO optionNetworkDTO = getOptionNetworkDTO();
                    BgmTvHttpProxyUpdateEvent bgmTvHttpProxyUpdateEvent =
                        new BgmTvHttpProxyUpdateEvent(this,
                            Boolean.valueOf(enableBgmTvHttpProxy),
                            value, optionNetworkDTO.getProxyHttpPort(),
                            optionNetworkDTO.getReadTimeout(),
                            optionNetworkDTO.getConnectTimeout());

                    MikanAndRssHttpProxyUpdateEvent mikanAndRssHttpProxyUpdateEvent =
                        new MikanAndRssHttpProxyUpdateEvent(this,
                            Boolean.valueOf(enableMikanHttpProxy),
                            value,
                            optionNetworkDTO.getProxyHttpPort(),
                            optionNetworkDTO.getReadTimeout(),
                            optionNetworkDTO.getConnectTimeout());

                    applicationContext.publishEvent(bgmTvHttpProxyUpdateEvent);
                    log.debug("publish EpisodeUrlUpdateEvent");
                    applicationContext.publishEvent(mikanAndRssHttpProxyUpdateEvent);
                    log.debug("publish MikanAndRssHttpProxyUpdateEvent");
                }
                if (OptionNetwork.PROXY_HTTP_PORT.name().equalsIgnoreCase(key)) {
                    OptionNetworkDTO optionNetworkDTO = getOptionNetworkDTO();
                    BgmTvHttpProxyUpdateEvent bgmTvHttpProxyUpdateEvent =
                        new BgmTvHttpProxyUpdateEvent(this,
                            Boolean.valueOf(enableBgmTvHttpProxy),
                            optionNetworkDTO.getProxyHttpHost(),
                            Integer.parseInt(value),
                            optionNetworkDTO.getReadTimeout(),
                            optionNetworkDTO.getConnectTimeout());

                    MikanAndRssHttpProxyUpdateEvent mikanAndRssHttpProxyUpdateEvent =
                        new MikanAndRssHttpProxyUpdateEvent(this,
                            Boolean.valueOf(enableMikanHttpProxy),
                            optionNetworkDTO.getProxyHttpHost(),
                            Integer.parseInt(value),
                            optionNetworkDTO.getReadTimeout(),
                            optionNetworkDTO.getConnectTimeout());

                    applicationContext.publishEvent(bgmTvHttpProxyUpdateEvent);
                    log.debug("publish EpisodeUrlUpdateEvent");
                    applicationContext.publishEvent(mikanAndRssHttpProxyUpdateEvent);
                    log.debug("publish MikanAndRssHttpProxyUpdateEvent");
                }
            }

            if (value == null) {
                log.warn("skip null value update for category={}, key={}, value={}",
                    category, key, value);
                continue;
            }

            OptionEntity optionEntity =
                optionRepository.findByCategoryAndKeyAndStatus(optionCategory, key, true);

            if (optionEntity != null) {
                optionEntity.setValue(value);
            } else {
                optionEntity = new OptionEntity();
                optionEntity.setCategory(optionCategory);
                optionEntity.setKey(key);
                optionEntity.setValue(value);
            }
            optionEntity = save(optionEntity);


            if (OptionCategory.QBITTORRENT.equals(optionCategory)) {
                applicationContext.publishEvent(new QbittorrentOptionUpdateEvent(this));
                log.debug("publish QbittorrentOptionUpdateEvent");
            }

            if (OptionCategory.NETWORK.equals(optionCategory)) {
                applicationContext.publishEvent(
                    new OptionNetworkUpdateEvent(this, getOptionNetworkDTO()));
                log.debug("publish OptionNetworkUpdateEvent");
            }

            if (OptionCategory.NOTIFY.equals(optionCategory)) {
                applicationContext.publishEvent(
                    new OptionNotifyUpdateEvent(this, getOptionNotifyDTO()));
                log.debug("publish OptionNotifyUpdateEvent");
            }

            OptionDTO optionDTO = new OptionDTO();
            optionDTO.setCategory(category);
            optionDTO.setKey(optionEntity.getKey());
            optionDTO.setValue(optionEntity.getValue());
            optionDTOList.add(optionDTO);
        }
        return optionDTOList;
    }

    private void checkNetworkHttpProxyHasConfig() {
        String httpHost = null;
        String httpPort = null;

        OptionEntity httpProxyHostOptionEntity =
            findOptionValueByCategoryAndKey(OptionCategory.NETWORK,
                OptionNetwork.PROXY_HTTP_HOST.name());
        if (httpProxyHostOptionEntity != null) {
            httpHost = httpProxyHostOptionEntity.getValue();
        }

        OptionEntity httpPortOptionEntity =
            findOptionValueByCategoryAndKey(OptionCategory.NETWORK,
                OptionNetwork.PROXY_HTTP_PORT.name());
        if (httpPortOptionEntity != null) {
            httpPort = httpPortOptionEntity.getValue();
        }

        if (StringUtils.isBlank(httpHost) || StringUtils.isBlank(httpPort)) {
            throw new IllegalArgumentException("please config http proxy host and port "
                + "in network option");
        }

    }

    private void checkAppEnableAutoAnimeSubTaskRunEnv() {
        // 1. 蜜柑计划RSS订阅已经配置
        OptionEntity mikanUrlOptionEntity =
            findOptionValueByCategoryAndKey(OptionCategory.MIKAN,
                OptionMikan.MY_SUBSCRIBE_RSS.name());
        if (mikanUrlOptionEntity == null || StringUtils.isBlank(mikanUrlOptionEntity.getValue())) {
            throw new IllegalArgumentException("please add mikan config such as subscribe rss url");
        }

        // 2. Qbittorrent的URL已经配置
        OptionEntity qbittorrentUrlOptionEntity =
            findOptionValueByCategoryAndKey(OptionCategory.QBITTORRENT,
                OptionQbittorrent.URL.name());
        if (qbittorrentUrlOptionEntity == null
            || StringUtils.isBlank(qbittorrentUrlOptionEntity.getValue())) {
            throw new IllegalArgumentException("please add qbittorrent config such as url");
        }
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext)
        throws BeansException {
        AssertUtils.notNull(applicationContext, "applicationContext");
        this.applicationContext = applicationContext;
    }


    @Nonnull
    @Override
    public OptionNetworkDTO getOptionNetworkDTO() {
        OptionNetworkDTO optionNetworkDTO = new OptionNetworkDTO();

        List<OptionEntity> optionEntityList = findOptionByCategory(OptionCategory.NETWORK);
        for (OptionEntity optionEntity : optionEntityList) {
            final String key = optionEntity.getKey();
            final String value = optionEntity.getValue();
            if (OptionNetwork.PROXY_HTTP_HOST.name().equalsIgnoreCase(key)) {
                optionNetworkDTO.setProxyHttpHost(value);
            }
            if (OptionNetwork.PROXY_HTTP_PORT.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                optionNetworkDTO.setProxyHttpPort(Integer.parseInt(value));
            }
            if (OptionNetwork.READ_TIMEOUT.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                optionNetworkDTO.setReadTimeout(Integer.parseInt(value));
            }
            if (OptionNetwork.CONNECT_TIMEOUT.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                optionNetworkDTO.setConnectTimeout(Integer.parseInt(value));
            }
        }
        return optionNetworkDTO;
    }

    @Nonnull
    @Override
    public OptionQbittorrentDTO getOptionQbittorrentDTO() {
        OptionQbittorrentDTO qbittorrentDTO = new OptionQbittorrentDTO();
        List<OptionEntity> optionEntityList = findOptionByCategory(OptionCategory.QBITTORRENT);
        for (OptionEntity optionEntity : optionEntityList) {
            final String key = optionEntity.getKey();
            final String value = optionEntity.getValue();
            if (OptionQbittorrent.URL.name().equalsIgnoreCase(key)) {
                qbittorrentDTO.setUrlPrefix(value);
            }
            if (OptionQbittorrent.ENABLE_AUTH.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                qbittorrentDTO.setEnableAuth(Boolean.valueOf(value));
            }
            if (OptionQbittorrent.USERNAME.name().equalsIgnoreCase(key)) {
                qbittorrentDTO.setUsername(value);
            }
            if (OptionQbittorrent.PASSWORD.name().equalsIgnoreCase(key)) {
                qbittorrentDTO.setPassword(value);
            }
        }
        return qbittorrentDTO;
    }

    @Nonnull
    @Override
    public OptionNotifyDTO getOptionNotifyDTO() {
        OptionNotifyDTO notifyDTO = new OptionNotifyDTO();
        List<OptionEntity> optionEntityList = findOptionByCategory(OptionCategory.NOTIFY);
        for (OptionEntity optionEntity : optionEntityList) {
            final String key = optionEntity.getKey();
            final String value = optionEntity.getValue();
            if (OptionNotify.MAIL_ENABLE.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                notifyDTO.setMailEnable(Boolean.valueOf(value));
            }
            if (OptionNotify.MAIL_PROTOCOL.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                notifyDTO.setMailProtocol(MailProtocol.valueOf(value));
            }
            if (OptionNotify.MAIL_SMTP_HOST.name().equalsIgnoreCase(key)) {
                notifyDTO.setMailSmtpHost(value);
            }
            if (OptionNotify.MAIL_SMTP_PORT.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                notifyDTO.setMailSmtpPort(Integer.valueOf(value));
            }
            if (OptionNotify.MAIL_SMTP_ACCOUNT.name().equalsIgnoreCase(key)) {
                notifyDTO.setMailSmtpAccount(value);
            }
            if (OptionNotify.MAIL_SMTP_ACCOUNT_ALIAS.name().equalsIgnoreCase(key)) {
                notifyDTO.setMailSmtpAccountAlias(value);
            }
            if (OptionNotify.MAIL_SMTP_PASSWORD.name().equalsIgnoreCase(key)) {
                notifyDTO.setMailSmtpPassword(value);
            }
        }
        return notifyDTO;
    }

    @Override
    public OptionBgmTvDTO getOptionBgmTvDTO() {
        OptionBgmTvDTO bgmTvDTO = new OptionBgmTvDTO();
        List<OptionEntity> optionEntityList = findOptionByCategory(OptionCategory.BGMTV);
        for (OptionEntity optionEntity : optionEntityList) {
            final String key = optionEntity.getKey();
            final String value = optionEntity.getValue();
            if (OptionBgmTv.ENABLE_PROXY.name().equalsIgnoreCase(key)
                && StringUtils.isNotBlank(value)) {
                Boolean enableProxy = Boolean.valueOf(value);
                bgmTvDTO.setEnableProxy(enableProxy);
            }
            if (OptionBgmTv.ACCESS_TOKEN.name().equalsIgnoreCase(key)) {
                bgmTvDTO.setAccessToken(value);
            }
        }
        return bgmTvDTO;
    }
}
