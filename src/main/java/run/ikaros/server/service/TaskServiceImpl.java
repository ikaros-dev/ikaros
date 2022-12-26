package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.KVService;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.RssService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.core.service.SubscribeService;
import run.ikaros.server.core.service.TaskService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.core.tripartite.bgmtv.constants.BgmTvConst;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.BaseEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.KVEntity;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.entity.SubscribeEntity;
import run.ikaros.server.entity.UserEntity;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.FileType;
import run.ikaros.server.enums.KVType;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionJellyfin;
import run.ikaros.server.enums.OptionMikan;
import run.ikaros.server.enums.SubscribeType;
import run.ikaros.server.exceptions.RegexMatchingException;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.params.SeasonMatchingEpParams;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;
import run.ikaros.server.tripartite.dmhy.DmhyClient;
import run.ikaros.server.tripartite.dmhy.enums.DmhyCategory;
import run.ikaros.server.tripartite.dmhy.model.DmhyRssItem;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;
import run.ikaros.server.tripartite.qbittorrent.enums.QbTorrentInfoFilter;
import run.ikaros.server.tripartite.qbittorrent.model.QbTorrentInfo;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.FileUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.RegexUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.SystemVarUtils;
import run.ikaros.server.utils.XmlUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static run.ikaros.server.constants.RegexConst.NUMBER_SEASON_SEQUENCE_WITH_PREFIX;

/**
 * @author li-guohao
 */
@Service
public class TaskServiceImpl implements TaskService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final RssService rssService;
    private final OptionService optionService;
    private final MikanService mikanService;
    private final BgmTvService bgmTvService;
    private final FileService fileService;
    private final SeasonService seasonService;
    private final AnimeService animeService;
    private final KVService kvService;
    private final QbittorrentClient qbittorrentClient;
    private final DmhyClient dmhyClient;
    private final SubscribeService subscribeService;
    private final UserService userService;

    private Set<String> hasHandledTorrentHashSet = new HashSet<>();
    private Set<Long> hasHandledSubscribeSet = new HashSet<>();

    public TaskServiceImpl(RssService rssService, OptionService optionService,
                           MikanService mikanService, BgmTvService bgmTvService,
                           FileService fileService, SeasonService seasonService,
                           AnimeService animeService,
                           KVService kvService,
                           QbittorrentClient qbittorrentClient, DmhyClient dmhyClient,
                           SubscribeService subscribeService, UserService userService) {
        this.rssService = rssService;
        this.optionService = optionService;
        this.mikanService = mikanService;
        this.bgmTvService = bgmTvService;
        this.fileService = fileService;
        this.seasonService = seasonService;
        this.animeService = animeService;
        this.kvService = kvService;
        this.qbittorrentClient = qbittorrentClient;
        this.dmhyClient = dmhyClient;
        this.subscribeService = subscribeService;
        this.userService = userService;
    }

    @Override
    public void pullMikanRssAnimeSubscribeAndSaveMetadataAndDownloadTorrents() {
        LOGGER.info("exec task: pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents");

        OptionEntity mikanSubRssOptionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.MIKAN,
                OptionMikan.MY_SUBSCRIBE_RSS.name());
        if (mikanSubRssOptionEntity == null
            || StringUtils.isBlank(mikanSubRssOptionEntity.getValue())) {
            throw new RuntimeIkarosException("please config mikan sub rss url");
        }

        LOGGER.info("start parse mikan my subscribe rss url from db");
        List<MikanRssItem> mikanRssItemList =
            rssService.parseMikanMySubscribeRss(mikanSubRssOptionEntity.getValue());
        LOGGER.info("parse mikan my subscribe rss url to mikan rss item list ");

        // 用户指定的特征匹配过滤
        UserEntity userOnlyOne = userService.getUserOnlyOne();
        if (userOnlyOne != null) {
            Long userId = userOnlyOne.getId();
            List<SubscribeEntity> userSubscribeEntityList =
                subscribeService.findByUserIdAndStatus(userId, true);
            for (SubscribeEntity userSubscribeEntity : userSubscribeEntityList) {
                String additional = userSubscribeEntity.getAdditional();
                if (StringUtils.isBlank(additional)) {
                    continue;
                }
                final List<String> tagList = RegexUtils.getFileTag(additional)
                    .stream()
                    .filter(tag -> !tag.matches(RegexConst.NUMBER_EPISODE_SEQUENCE))
                    .toList();

                if (StringUtils.isNotBlank(additional)) {
                    mikanRssItemList = mikanRssItemList.stream()
                        .filter(mikanRssItem -> {
                            String title = mikanRssItem.getTitle();
                            boolean result = true;
                            if (title.contains(
                                additional.replaceAll(RegexConst.FILE_NAME_TAG, ""))) {
                                for (String tag : tagList) {
                                    if (!title.contains(tag)) {
                                        result = false;
                                        break;
                                    }
                                }
                            }
                            return result;
                        }).collect(Collectors.toList());
                }
            }
        }

        for (MikanRssItem mikanRssItem : mikanRssItemList) {
            try {
                String mikanRssItemTitle = mikanRssItem.getTitle();
                LOGGER.info("start for each mikan rss item list for item title: {}",
                    mikanRssItemTitle);

                qbittorrentClient.addTorrentFromUrl(mikanRssItem.getTorrentUrl(),
                    mikanRssItemTitle);
                LOGGER.info("add to qbittorrent for torrent name: {}", mikanRssItemTitle);

                String episodePageUrl = mikanRssItem.getEpisodePageUrl();
                String bgmtvSubjectId =
                    findBgmTvSubjectIdByTorrentName(mikanRssItemTitle, episodePageUrl);
                if (bgmtvSubjectId == null) {
                    LOGGER.error("skip current item for title={}", mikanRssItemTitle);
                    continue;
                }

                kvService.save(new KVEntity()
                    .setType(KVType.MIKAN_EP_URL__BGM_TV_SUBJECT_ID)
                    .setKey(episodePageUrl)
                    .setValue(bgmtvSubjectId)
                );
                LOGGER.debug("save new relation for mikan ep url and bgmtv subject id, "
                    + "episodePageUrl={}, bgmtvSubjectId={}", episodePageUrl, bgmtvSubjectId);

                kvService.save(new KVEntity()
                    .setType(KVType.MIKAN_TORRENT_NAME__BGM_TV_SUBJECT_ID)
                    .setKey(mikanRssItemTitle)
                    .setValue(bgmtvSubjectId)
                );
                LOGGER.debug("save new relation for mikan ep url and bgmtv subject id, "
                        + "torrentNameUrl={}, bgmtvSubjectId={}", mikanRssItemTitle,
                    bgmtvSubjectId);


                try {
                    bgmTvService.reqBgmtvSubject(Long.valueOf(bgmtvSubjectId));
                } catch (Exception exception) {
                    throw new RuntimeIkarosException(
                        "request bgmtv subject and save new anime entity fail", exception);
                }
            } catch (Exception exception) {
                LOGGER.warn("handle current mikan rss item fail, item={}",
                    JsonUtils.obj2Json(mikanRssItem), exception);
            }
        }

        // 如果新添加的种子文件状态是缺失文件，则需要再恢复下
        qbittorrentClient.tryToResumeAllMissingFilesErroredTorrents();

    }

    private String findBgmTvSubjectIdByTorrentName(@Nonnull String torrentName,
                                                   @Nullable String episodePageUrl) {
        AssertUtils.notBlank(torrentName, "torrentName");
        String bgmtvSubjectId = null;
        Map<String, String> mikanTorrentNameBgmTvSubjectIdMap =
            kvService.findMikanTorrentNameBgmTvSubjectIdMap();
        if (mikanTorrentNameBgmTvSubjectIdMap.containsKey(torrentName)) {
            bgmtvSubjectId = mikanTorrentNameBgmTvSubjectIdMap.get(torrentName);
            LOGGER.debug("find exist relation for mikan torrent name and bgmtv subject id, "
                    + "torrentName={}, bgmtvSubjectId={}", torrentName,
                bgmtvSubjectId);
        } else {
            // 这里由于bgmtv的旧查询API可用，直接解析标题，作为关键词查询对应的条目ID
            // 标题例子：[Lilith-Raws] 孤独摇滚！ / Bocchi the Rock!
            // - 06 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]

            // 通过密柑剧集URL请求密柑页面获取对应的 bgmtv条目ID
            if (StringUtils.isNotBlank(episodePageUrl)) {
                bgmtvSubjectId = findBgmTvSubjectIdByEpisodePageUrl(episodePageUrl);
            }

            if (StringUtils.isNotBlank(bgmtvSubjectId)) {
                return bgmtvSubjectId;
            }

            String enName = null;
            String chName = null;
            try {
                enName = RegexUtils.getMatchingEnglishStrWithoutTag(torrentName);
                chName = RegexUtils.getMatchingChineseStrWithoutTag(torrentName);
            } catch (RegexMatchingException regexMatchingException) {
                LOGGER.warn("matching fail for torrentName={}", torrentName);
            }

            Optional<BgmTvSubject> enNameSubOptional = Optional.empty();
            if (StringUtils.isNotBlank(enName)) {
                LOGGER.debug("search bgmtv subject by english name staring ...");
                enNameSubOptional =
                    bgmTvService.searchSubject(enName, BgmTvSubjectType.ANIME)
                        .stream().findFirst();
            }
            if (enNameSubOptional.isPresent()) {
                bgmtvSubjectId = String.valueOf(enNameSubOptional.get().getId());
                LOGGER.info("search bgmtv subject by english name success,"
                    + "bgmtvSubjectId={}, enName={}", bgmtvSubjectId, enName);
            } else {
                Optional<BgmTvSubject> chNameSubOptional = Optional.empty();
                if (StringUtils.isNotBlank(chName)) {
                    chNameSubOptional =
                        bgmTvService.searchSubject(chName, BgmTvSubjectType.ANIME)
                            .stream().findFirst();
                }
                if (chNameSubOptional.isPresent()) {
                    bgmtvSubjectId = String.valueOf(chNameSubOptional.get().getId());
                    LOGGER.info("search bgmtv subject by chinese name success,"
                        + "bgmtvSubjectId={}, chName={}", bgmtvSubjectId, chName);
                }
            }

        }

        if (StringUtils.isBlank(bgmtvSubjectId)) {
            LOGGER.error("search bgmtvSubjectId fail torrentName={}; episodePageUrl={}",
                torrentName, episodePageUrl);
        }

        return bgmtvSubjectId;
    }

    private String findBgmTvSubjectIdByEpisodePageUrl(String episodePageUrl) {
        AssertUtils.notBlank(episodePageUrl, "episodePageUrl");
        String animePageUrl =
            mikanService.getAnimePageUrlByEpisodePageUrl(episodePageUrl);
        String bgmTvSubjectPageUrl =
            mikanService.getBgmTvSubjectPageUrlByAnimePageUrl(animePageUrl);
        String bgmtvSubjectId =
            bgmTvSubjectPageUrl.substring(
                bgmTvSubjectPageUrl.lastIndexOf("/") + 1);
        LOGGER.info(
            "search bgmtv subject by mikan episode to anime page success,"
                + "bgmtvSubjectId={}", bgmtvSubjectId);
        return bgmtvSubjectId;
    }

    @Override
    public void searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode() {
        LOGGER.info("exec task: searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode");
        List<QbTorrentInfo> torrentList = qbittorrentClient.getTorrentList(QbTorrentInfoFilter.ALL,
            qbittorrentClient.getCategory(), null, null, null, null);

        List<QbTorrentInfo> downloadProcessFinishTorrentList
            = torrentList.stream()
            .filter(qbTorrentInfo -> qbTorrentInfo.getProgress() == 1.0)
            .toList();

        for (QbTorrentInfo qbTorrentInfo : downloadProcessFinishTorrentList) {
            final String hash = qbTorrentInfo.getHash();
            if (hasHandledTorrentHashSet.contains(hash)) {
                continue;
            }

            String contentPath = qbTorrentInfo.getContentPath();
            String name = qbTorrentInfo.getName();

            // 创建两个文件硬链接：服务器上传目录 和 Jellyfin目录
            try {
                createServerFileHardLink(name, contentPath);
                // LOGGER.info("create server file hard link success for name={}", name);
            } catch (Exception exception) {
                LOGGER.warn("create server file hard link fail", exception);
            }
            // try {
            //     createJellyfinFileHardLink(name, contentPath);
            //     LOGGER.info("create jellyfin file hard link success for name={}", name);
            // } catch (Exception exception) {
            //     LOGGER.warn("create jellyfin file hard link fail", exception);
            // }

            // 创建原始目录
            try {
                createOriginalFileHardLink(name, contentPath);
                // LOGGER.info("create original dir file hard link success for name={}", name);
            } catch (Exception exception) {
                LOGGER.warn("create original dir file hard link fail", exception);
            }

            hasHandledTorrentHashSet.add(hash);
        }
        LOGGER.info("end task: searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode");
    }

    private void createOriginalFileHardLink(String torrentName, String torrentContentPath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(torrentContentPath, "torrentContentPath");

        // 如果目录不存在，则创建
        final String originalDirPath = SystemVarUtils.getCurrentAppOriginalDirPath();
        File originalDir = new File(originalDirPath);
        if (!originalDir.exists()) {
            originalDir.mkdirs();
        }

        // 硬链接创建种子原始目录
        // qbittorrent download path => ikaros app download path
        // /downloads/xxx => /opt/ikaros/downloads/xxx
        torrentContentPath = addPrefixForQbittorrentDownloadPath(torrentContentPath);
        File torrentContentFile = new File(torrentContentPath);
        createOriginalFileHardLinkRecursively(torrentContentFile,
            SystemVarUtils.getCurrentAppOriginalDirPath());
    }

    private void createOriginalFileHardLinkRecursively(File torrentContentFile, String dirPath) {
        AssertUtils.notNull(torrentContentFile, "torrentContentFile");
        AssertUtils.notBlank(dirPath, "dir path");
        if (torrentContentFile.isDirectory()) {
            dirPath = dirPath + File.separator
                + FileUtils.formatDirName(torrentContentFile.getName());
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            for (File file : Objects.requireNonNull(torrentContentFile.listFiles())) {
                createOriginalFileHardLinkRecursively(file, dirPath);
            }
        } else {
            // 单个文件
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String originalFilePath = dirPath
                + File.separator + torrentContentFile.getName();
            String torrentContentPath = torrentContentFile.getAbsolutePath();
            File originalFile = new File(originalFilePath);
            if (originalFile.exists()) {
                // LOGGER.debug("skip, file exists for path: {}", originalFilePath);
                return;
            }
            try {
                Files.createLink(originalFile.toPath(), torrentContentFile.toPath());
                LOGGER.debug("success create origin file hard link, link: {}, existing: {}",
                    originalFilePath, torrentContentPath);
            } catch (IOException e) {
                LOGGER.warn("success create origin file hard link, link: {}, existing: {}",
                    originalFilePath, torrentContentPath, e);
            }
        }
    }

    private String getBgmTvSubjectIdByMikanSearchTorrentName(@Nonnull String torrentName) {
        AssertUtils.notBlank(torrentName, "torrentName");
        FileType fileType = FileUtils.parseTypeByPostfix(FileUtils.parseFilePostfix(torrentName));
        if (fileType != FileType.VIDEO) {
            return null;
        }

        // 调用蜜柑计划查询页面，获取Bangumi页面地址
        String animePageUrl = mikanService.getAnimePageUrlBySearch(torrentName);
        if (StringUtils.isBlank(animePageUrl)) {
            return null;
        }
        String subjectPageUrl =
            mikanService.getBgmTvSubjectPageUrlByAnimePageUrl(animePageUrl);
        String subjectIdStr = subjectPageUrl.replace(BgmTvConst.SUBJECT_PREFIX, "");
        KVEntity kvEntity = new KVEntity();
        kvEntity.setType(KVType.MIKAN_TORRENT_NAME__BGM_TV_SUBJECT_ID);
        kvEntity.setKey(torrentName);
        kvEntity.setValue(subjectIdStr);
        kvService.save(kvEntity);
        LOGGER.debug("save new relation for mikan torrent name and bgmtv subject id, "
            + "torrent name: {}, bgmtv subject id: {}", torrentName, subjectIdStr);
        return subjectIdStr;
    }

    private void updateEpisodeUrlByFileEntity(String torrentName, Long fileId) {
        Map<String, String> torrentNameBgmTvSubjectIdMap =
            kvService.findMikanTorrentNameBgmTvSubjectIdMap();
        String subjectIdStr = null;
        if (torrentNameBgmTvSubjectIdMap.containsKey(
            torrentName.replace(RegexConst.FILE_POSTFIX, "")
        )) {
            subjectIdStr = torrentNameBgmTvSubjectIdMap.get(torrentName);
            if (StringUtils.isBlank(subjectIdStr)) {
                subjectIdStr = getBgmTvSubjectIdByMikanSearchTorrentName(torrentName);
            }
        }

        if (StringUtils.isBlank(subjectIdStr)) {
            LOGGER.warn("skip matching because of subjectIdStr is blank");
            return;
        }
        Long subjectId = Long.valueOf(subjectIdStr);
        Long animeId;
        AnimeEntity animeEntity = animeService.findByBgmTvId(subjectId);
        if (animeEntity == null) {
            AnimeDTO animeDTO = bgmTvService.reqBgmtvSubject(subjectId);
            if (animeDTO == null) {
                LOGGER.warn("anime dto not found for subjectId={},"
                        + " skip matching episode url by file entity for torrent name={}",
                    subjectId,
                    torrentName);
                return;
            }
            animeId = animeDTO.getId();
        } else {
            animeId = animeEntity.getId();
        }
        List<SeasonEntity> seasonEntityList = seasonService.findByAnimeId(animeId);
        if (seasonEntityList.isEmpty()) {
            LOGGER.warn("season not found for animeId={}, "
                    + "skip matching episode url by file entity for torrent name={}",
                animeId,
                torrentName);
            return;
        }

        SeasonEntity seasonEntity = seasonEntityList.get(0);
        SeasonMatchingEpParams seasonMatchingEpParams = new SeasonMatchingEpParams();
        seasonMatchingEpParams.setSeasonId(seasonEntity.getId());
        seasonMatchingEpParams.setFileIdList(List.of(fileId));
        seasonMatchingEpParams.setIsNotify(true);
        seasonService.matchingEpisodesUrlByFileIds(seasonMatchingEpParams);
    }

    private void createServerFileHardLinkRecursively(File torrentContentFile) {
        AssertUtils.notNull(torrentContentFile, "torrentContentFile");
        if (torrentContentFile.isDirectory()) {
            for (File file : Objects.requireNonNull(torrentContentFile.listFiles())) {
                createServerFileHardLinkRecursively(file);
            }
        } else {
            // 单个文件
            String fileName = torrentContentFile.getName();
            String postfix = FileUtils.parseFilePostfix(fileName);
            String serverFilePath
                = FileUtils.buildAppUploadFilePath(postfix);

            List<FileEntity> existsFileEntityList
                = fileService
                .findListByName(fileName)
                .stream()
                .filter(BaseEntity::getStatus)
                .toList();
            if (!existsFileEntityList.isEmpty()) {
                // LOGGER.debug("skip, file exists for path: {}", serverFilePath);
                return;
            }

            String torrentContentPath = torrentContentFile.getAbsolutePath();
            File serverFile = new File(serverFilePath);
            if (serverFile.exists()) {
                // LOGGER.debug("skip, file exists for path: {}", serverFilePath);
                return;
            }
            try {
                Files.createLink(serverFile.toPath(), torrentContentFile.toPath());
                LOGGER.debug("success create server file hard link, link: {}, existing: {}",
                    serverFilePath, torrentContentPath);
                FileEntity fileEntity = new FileEntity();
                fileEntity.setName(fileName);
                fileEntity.setDirName(torrentContentFile.getParentFile().getName());
                fileEntity.setUrl(serverFilePath.replace(
                    SystemVarUtils.getCurrentAppDirPath(), ""));
                fileEntity.setOriginalPath(torrentContentPath);
                fileEntity.setType(FileUtils.parseTypeByPostfix(postfix));
                fileEntity.setPlace(FilePlace.LOCAL);
                fileEntity.setSize(torrentContentFile.length());
                fileEntity = fileService.create(fileEntity);
                LOGGER.debug("save new file entity for name={}", fileName);

                updateEpisodeUrlByFileEntity(torrentContentFile.getName(), fileEntity.getId());
            } catch (RegexMatchingException regexMatchingException) {
                LOGGER.warn("regex matching fail, msg: {}", regexMatchingException.getMessage());
            } catch (Exception e) {
                LOGGER.error(
                    "fail create server file hard link, exception: ", e);
            }
        }
    }

    private void createServerFileHardLink(String torrentName, String torrentContentPath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(torrentContentPath, "torrentContentPath");
        // qbittorrent download path => ikaros app download path
        // /downloads/xxx => /opt/ikaros/downloads/xxx
        torrentContentPath = addPrefixForQbittorrentDownloadPath(torrentContentPath);
        File torrentContentFile = new File(torrentContentPath);
        createServerFileHardLinkRecursively(torrentContentFile);
    }

    private static String addPrefixForQbittorrentDownloadPath(String torrentContentPath) {
        return SystemVarUtils.getCurrentAppDirPath()
            + (torrentContentPath.startsWith(String.valueOf(File.separatorChar))
            ? torrentContentPath : (File.separatorChar + torrentContentPath));
    }

    private void createJellyfinFileHardLink(String torrentName, String torrentContentPath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(torrentContentPath, "torrentContentPath");
        torrentContentPath = addPrefixForQbittorrentDownloadPath(torrentContentPath);

        // 查本地数据库关系表
        Map<String, String> mikanTorrentNameBgmTvSubjectIdMap =
            kvService.findMikanTorrentNameBgmTvSubjectIdMap();
        Map<String, String> bgmTvSubjectIdMikanEpUrlMap =
            kvService.findBgmTvSubjectIdMikanEpUrlMap();
        String bgmtvSubjectId = mikanTorrentNameBgmTvSubjectIdMap.get(torrentName);
        if (StringUtils.isBlank(bgmtvSubjectId)) {
            String mikanEpUrl = bgmTvSubjectIdMikanEpUrlMap.get(bgmtvSubjectId);
            bgmtvSubjectId = findBgmTvSubjectIdByTorrentName(torrentName, mikanEpUrl);
            kvService.save(new KVEntity()
                .setType(KVType.MIKAN_TORRENT_NAME__BGM_TV_SUBJECT_ID)
                .setKey(torrentName)
                .setValue(bgmtvSubjectId));
        }


        OptionEntity jellyfinMediaPathOptionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.JELLYFIN,
                OptionJellyfin.MEDIA_DIR_PATH.name());
        if (StringUtils.isBlank(jellyfinMediaPathOptionEntity.getValue())) {
            throw new RuntimeIkarosException("please config jellyfin media path");
        }
        final String jellyfinMediaBasePath = jellyfinMediaPathOptionEntity.getValue();

        if (bgmtvSubjectId == null) {
            LOGGER.error("skip current anime for torrentName={}, bgmTvSubjectId={}",
                torrentName, bgmtvSubjectId);
            return;
        }

        String dirName = buildMediaAnimeDirName(bgmtvSubjectId);
        if (dirName == null) {
            LOGGER.error("skip current anime for torrentName={}, bgmTvSubjectId={}",
                torrentName, bgmtvSubjectId);
            return;
        }

        String jellyfinMediaDirPath = jellyfinMediaBasePath
            + File.separatorChar + dirName;

        // jellyfin media path => ikaros app media path
        // /media/xxx => /opt/ikaros/media/xxx
        jellyfinMediaDirPath = SystemVarUtils.getCurrentAppDirPath()
            + (jellyfinMediaDirPath.startsWith("/")
            ? jellyfinMediaDirPath : (File.separatorChar + jellyfinMediaDirPath));

        File jellyfinMediaDir = new File(jellyfinMediaDirPath);
        if (!jellyfinMediaDir.exists()) {
            jellyfinMediaDir.mkdirs();
        }


        // 季度封面和tvshow.nfo
        // 用目录名称，根据标题模糊查询数据库季度表
        Long subjectId = Long.valueOf(bgmtvSubjectId);
        AnimeEntity animeEntity = animeService.findByBgmTvId(subjectId);
        if (animeEntity == null) {
            AnimeDTO animeDTO = bgmTvService.reqBgmtvSubject(subjectId);
            animeEntity = animeService.getById(animeDTO.getId());
            if (animeEntity == null) {
                return;
            }
        }
        String tvshowNfoFilePath = jellyfinMediaDir + File.separator + "tvshow.nfo";
        File tvshowNfoFile = new File(tvshowNfoFilePath);
        if (!tvshowNfoFile.exists()) {
            XmlUtils.generateJellyfinTvShowNfoXml(tvshowNfoFilePath,
                animeEntity.getOverview(), animeEntity.getTitleCn(),
                animeEntity.getTitle(),
                String.valueOf(animeEntity.getBgmtvId()));
        }
        String coverUrl = animeEntity.getCoverUrl();
        String coverFilePath = SystemVarUtils.getCurrentAppDirPath()
            + (coverUrl.startsWith(File.separator) ? coverUrl : File.separator + coverUrl);
        File coverFile = new File(coverFilePath);
        if (coverFile.exists()) {
            String postfix = FileUtils.parseFilePostfix(coverFilePath);
            String posterFilePath = jellyfinMediaDirPath
                + File.separator
                + "poster"
                + (StringUtils.isBlank(postfix) ? ".jpg" :
                postfix.startsWith(".") ? postfix : "." + postfix);
            File posterFile = new File(posterFilePath);
            if (!posterFile.exists()) {
                try {
                    Files.createLink(posterFile.toPath(), coverFile.toPath());
                    LOGGER.warn(
                        "create jellyfin poster.jpg hard link success, link={}, existing={}",
                        posterFilePath, coverFilePath);
                } catch (IOException e) {
                    LOGGER.warn(
                        "create jellyfin poster.jpg hard link fail, link={}, existing={}",
                        posterFilePath, coverFilePath);
                }
            }
        } else {
            LOGGER.warn(
                "cover file not exist, skip create poster.jpg for jellyfinMediaDirPath: {}",
                jellyfinMediaDirPath);
        }

        // 处理文件
        File torrentContentFile = new File(torrentContentPath);
        if (torrentContentFile.isDirectory()) {
            // 目录多文件，则加上统一前缀
            File[] files = torrentContentFile.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    fileName = fileName.replaceAll(NUMBER_SEASON_SEQUENCE_WITH_PREFIX, "");
                    Long seq = null;
                    try {
                        seq = RegexUtils.getFileNameTagEpSeq(fileName);
                    } catch (RegexMatchingException regexMatchingException) {
                        LOGGER.warn("matching fail for fileName={}, ex msg:{}", fileName,
                            regexMatchingException.getMessage());
                    }
                    if (seq != null) {
                        fileName = "S1E" + seq + "-" + fileName;
                    }
                    String jellyfinFilePath = jellyfinMediaDirPath + File.separatorChar + fileName;
                    File jellyfinFile = new File(jellyfinFilePath);
                    if (jellyfinFile.exists()) {
                        continue;
                    }
                    Files.createLink(jellyfinFile.toPath(), file.toPath());
                    LOGGER.debug("copy jellyfin file hard link success, link={}, existing={}",
                        jellyfinFilePath, torrentContentPath);

                } catch (RegexMatchingException regexMatchingException) {
                    LOGGER.warn("regex matching fail, msg: {}",
                        regexMatchingException.getMessage());
                } catch (Exception e) {
                    LOGGER.error(
                        "create jellyfin file hard link fail, please let qbittorrent and ikaros "
                            + "and jellyfin instance in the same file system",
                        e);
                }
            }
        } else {
            // 单剧集文件
            String fileName = FileUtils.parseFileName(torrentContentPath);
            fileName = fileName.replaceAll(NUMBER_SEASON_SEQUENCE_WITH_PREFIX, "");
            Long seq = null;
            try {
                seq = RegexUtils.getFileNameTagEpSeq(fileName);
            } catch (RegexMatchingException regexMatchingException) {
                LOGGER.warn("get file name tage episode seq fail for filename={}", fileName);
            }

            if (seq != null) {
                fileName = "S1E" + seq + "-" + fileName;
            }
            String jellyfinFilePath = jellyfinMediaDirPath + File.separatorChar + fileName;
            File jellyfinFile = new File(jellyfinFilePath);

            try {
                if (jellyfinFile.exists()) {
                    return;
                }
                Files.createLink(jellyfinFile.toPath(), torrentContentFile.toPath());
                LOGGER.debug("copy jellyfin file hard link success, link={}, existing={}",
                    jellyfinFilePath, torrentContentPath);

            } catch (RegexMatchingException regexMatchingException) {
                LOGGER.warn("regex matching fail, msg: {}", regexMatchingException.getMessage());
            } catch (Exception e) {
                LOGGER.error(
                    "create jellyfin file hard link fail, please let qbittorrent and ikaros "
                        + "and jellyfin instance in the same file system",
                    e);
            }
        }
    }

    /**
     * 查询番组计划获取条目信息，以条目 中文名 - 英文名 (年月日) 这种格式生成媒体番剧目录，
     * 比如 孤独摇滚！- ぼっち・ざ・ろっく！(2022-10-08)
     *
     * @param bgmtvSubjectId 番组计划条目ID
     * @return 媒体目录名称
     */
    @Nullable
    private String buildMediaAnimeDirName(@Nonnull String bgmtvSubjectId) {
        AssertUtils.notBlank(bgmtvSubjectId, "bgmtvSubjectId");
        try {
            BgmTvSubject subject = bgmTvService.getSubject(Long.valueOf(bgmtvSubjectId));
            if (subject == null) {
                throw new RuntimeIkarosException("subject not found");
            }

            String nameCn = subject.getNameCn();
            String name = subject.getName();
            String date = subject.getDate();


            StringBuilder sb = new StringBuilder();
            sb.append(nameCn)
                .append(" - ")
                .append(name)
                .append(" (")
                .append(date)
                .append(")");

            return sb.toString();
        } catch (Exception exception) {
            LOGGER.error("search subject fail for id={}", bgmtvSubjectId);
            return null;
        }
    }


    @Override
    public void downloadSubscribeAnimeResource(@Nullable Long userId) {
        LOGGER.info("start exec task downloadSubscribeAnimeResource");
        if (userId == null) {
            UserEntity userOnlyOne = userService.getUserOnlyOne();
            if (userOnlyOne == null) {
                LOGGER.warn("skip exec task downloadSubscribeAnimeResource, no user in db");
                return;
            }
            userId = userOnlyOne.getId();
        }
        List<SubscribeEntity> subscribeEntityList =
            subscribeService.findByUserIdAndStatus(userId, true);
        for (SubscribeEntity userSubscribeEntity : subscribeEntityList) {
            handleSingleUserSubscribe(userSubscribeEntity);
        }
        LOGGER.info("end exec task downloadSubscribeAnimeResource");
    }

    @Override
    public void scanImportDir2ImportNewFile() {
        String importDirPath = SystemVarUtils.getCurrentImportDirPath();
        File importDir = new File(importDirPath);
        if (!importDir.exists()) {
            importDir.mkdirs();
            LOGGER.debug("mkdir import dir, path={}", importDirPath);
        }
        handleImportDirFile(importDir);
    }

    private void handleImportDirFile(File file) {
        if (file.isDirectory()) {
            // dir
            for (File f : Objects.requireNonNull(file.listFiles())) {
                handleImportDirFile(f);
            }
        } else {
            // file
            if (!file.exists()) {
                return;
            }

            // 获取相对于 import 目录的相对路径
            String absolutePath = file.getAbsolutePath();
            String dirRelativePath
                = absolutePath.replace(File.separator + AppConst.IMPORT, "");

            File originalFile = null;
            try {

                // 查询文件是否已经导入
                String name = file.getName();
                List<FileEntity> existsFileEntityList = fileService.findListByName(name);
                if (!existsFileEntityList.isEmpty()) {
                    return;
                }

                // 移动文件到 original 目录
                // originalFile = new File(SystemVarUtils.getCurrentAppDirPath()
                //     + File.separator + AppConst.ORIGINAL + dirRelativePath);
                // if (!originalFile.getParentFile().exists()) {
                //     originalFile.getParentFile().mkdirs();
                // }
                // if (originalFile.exists()) {
                //     originalFile.delete();
                // }
                // Files.move(file.toPath(), originalFile.toPath());
                // LOGGER.debug("move file from import dir to original dir,"
                //     + "form:{}, to:{}", file.getAbsolutePath(), originalFile.getAbsolutePath());

                // 创建上传文件目录
                String uploadFilePath
                    = FileUtils.buildAppUploadFilePath(FileUtils.parseFilePostfix(name));
                File uploadFile = new File(uploadFilePath);

                // 创建文件硬链接
                // Files.createLink(uploadFile.toPath(), originalFile.toPath());
                // LOGGER.debug("create upload file hard link form original dir, "
                //         + "link={}, existing={}",
                //     uploadFile.getAbsolutePath(), originalFile.getAbsolutePath());

                // 硬链接限制太多，暂时不进行支持，直接进行复制
                Files.copy(file.toPath(), uploadFile.toPath());
                LOGGER.debug("copy file from: {}, to: {}",
                    file.getAbsolutePath(), uploadFile.getAbsolutePath());

                FileEntity fileEntity = new FileEntity();
                fileEntity.setPlace(FilePlace.LOCAL);
                fileEntity.setDirName(file.getParentFile().getName());
                fileEntity.setName(name);
                fileEntity.setType(FileUtils.parseTypeByPostfix(FileUtils.parseFilePostfix(name)));
                fileEntity.setUrl(
                    uploadFilePath.replace(SystemVarUtils.getCurrentAppDirPath(), ""));
                fileEntity.setType(FileUtils.parseTypeByPostfix(FileUtils.parseFilePostfix(name)));
                fileEntity.setOriginalPath(uploadFilePath);
                fileService.save(fileEntity);
                LOGGER.debug("save file entity form original dir for file name={}", name);
                LOGGER.debug("success import file form path={}", file.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("exec handleImportDirFile exception", e);
            } finally {
                // if (originalFile != null && originalFile.exists()) {
                //     try {
                //         Files.move(originalFile.toPath(), file.toPath());
                //         LOGGER.debug("move file from original dir to import dir,"
                //             + "form:{}, to:{}",
                //             originalFile.getAbsolutePath(), file.getAbsolutePath());
                //     } catch (IOException e) {
                //         LOGGER.error("move file from origin dir to import dir fail, "
                //                 + " original dir file path={}, import dir file path={}",
                //             originalFile.getAbsolutePath(), file.getAbsolutePath());
                //     }
                // }
            }
        }
    }

    private void handleSingleUserSubscribe(SubscribeEntity subscribeEntity) {
        Long subscribeEntityId = subscribeEntity.getId();
        if (hasHandledSubscribeSet.contains(subscribeEntityId)) {
            return;
        }

        if (subscribeEntity.getType().equals(SubscribeType.ANIME)) {
            Long animeId = subscribeEntity.getTargetId();
            AnimeEntity animeEntity = animeService.getById(animeId);
            if (animeEntity == null) {
                LOGGER.warn("not found anime record for id={}", animeId);
                return;
            }
            String keyword = animeEntity.getTitleCn();
            if (StringUtils.isBlank(keyword)) {
                LOGGER.warn("current anime title cn is blank for anime id={}, title={}",
                    animeId, animeEntity.getTitle());
                return;
            }

            String additional = subscribeEntity.getAdditional();
            final List<String> tagList = RegexUtils.getFileTag(additional)
                .stream()
                .filter(tag -> !tag.matches(RegexConst.NUMBER_EPISODE_SEQUENCE))
                .toList();

            List<DmhyRssItem> dmhyRssItemList
                = dmhyClient.findRssItems(keyword, DmhyCategory.ANIME)
                .stream()
                .filter(dmhyRssItem -> {
                    String title = dmhyRssItem.getTitle();
                    boolean result = true;
                    for (String tag : tagList) {
                        if (!title.contains(tag)) {
                            result = false;
                            break;
                        }
                    }
                    return result;
                }).toList();
            for (DmhyRssItem dmhyRssItem : dmhyRssItemList) {
                String magnetUrl = dmhyRssItem.getMagnetUrl();
                if (StringUtils.isNotBlank(magnetUrl)) {
                    qbittorrentClient.addTorrentFromUrl(magnetUrl, dmhyRssItem.getTitle());
                }
            }
        }
        hasHandledSubscribeSet.add(subscribeEntityId);
    }
}
