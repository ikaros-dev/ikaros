package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.KVService;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.RssService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.core.service.TaskService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.KVEntity;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.KVType;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionJellyfin;
import run.ikaros.server.enums.OptionMikan;
import run.ikaros.server.exceptions.RegexMatchingException;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private Set<String> hasHandledTorrentHashSet = new HashSet<>();

    public TaskServiceImpl(RssService rssService, OptionService optionService,
                           MikanService mikanService, BgmTvService bgmTvService,
                           FileService fileService, SeasonService seasonService,
                           AnimeService animeService,
                           KVService kvService,
                           QbittorrentClient qbittorrentClient) {
        this.rssService = rssService;
        this.optionService = optionService;
        this.mikanService = mikanService;
        this.bgmTvService = bgmTvService;
        this.fileService = fileService;
        this.seasonService = seasonService;
        this.animeService = animeService;
        this.kvService = kvService;
        this.qbittorrentClient = qbittorrentClient;
    }

    @Override
    public void pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents() {
        LOGGER.info("exec task: pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents");

        OptionEntity mikanSubRssOptionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.MIKAN,
                OptionMikan.MY_SUBSCRIBE_RSS.name());
        if (StringUtils.isBlank(mikanSubRssOptionEntity.getValue())) {
            throw new RuntimeIkarosException("please config mikan sub rss url");
        }
        String mikanMySubscribeRssUrl = mikanSubRssOptionEntity.getValue();

        LOGGER.info("start parse mikan my subscribe rss url from db");
        List<MikanRssItem> mikanRssItemList =
            rssService.parseMikanMySubscribeRss(mikanMySubscribeRssUrl);
        LOGGER.info("parse mikan my subscribe rss url to mikan rss item list ");

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
                bgmtvSubjectId = findBgmTvSubjectIdByEpisdoePageUrl(episodePageUrl);
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

    private String findBgmTvSubjectIdByEpisdoePageUrl(String episodePageUrl) {
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
            } catch (RuntimeException runtimeException) {
                LOGGER.warn("create server file hard link fail", runtimeException);
            }
            try {
                createJellyfinFileHardLink(name, contentPath);
            } catch (RuntimeException runtimeException) {
                LOGGER.warn("create jellyfin file hard link fail", runtimeException);
            }

            hasHandledTorrentHashSet.add(hash);
        }


    }

    private void createServerFileHardLink(String torrentName, String torrentContentPath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(torrentContentPath, "torrentContentPath");
        // qbittorrent download path => ikaros app download path
        // /downloads/xxx => /opt/ikaros/downloads/xxx
        torrentContentPath = addPrefixForQbittorrentDownloadPath(torrentContentPath);
        File torrentContentFile = new File(torrentContentPath);
        if (torrentContentFile.isDirectory()) {
            // 是目录，则上传目录下的所有文件到服务器目录
            File[] files = torrentContentFile.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file : files) {
                String fileName = file.getName();
                String postfix = FileUtils.parseFilePostfix(fileName);
                String uploadFilePath = FileUtils.buildAppUploadFilePath(postfix);
                File uploadFile = new File(uploadFilePath);
                if (uploadFile.exists()) {
                    continue;
                }
                try {
                    Files.createLink(uploadFile.toPath(), file.toPath());
                    LOGGER.debug("copy server file hard link success, link={}, existing={}",
                        uploadFilePath, torrentContentPath);
                    FileEntity fileEntity = fileService.create(new FileEntity()
                        .setPlace(FilePlace.LOCAL)
                        .setUrl(uploadFilePath.replace(SystemVarUtils.getCurrentAppDirPath(), ""))
                        .setName(fileName)
                        .setType(FileUtils.parseTypeByPostfix(postfix)));

                    seasonService.updateEpisodeUrlByFileEntity(fileEntity);
                } catch (RegexMatchingException regexMatchingException) {
                    LOGGER.warn("regex matching fail, msg: {}",
                        regexMatchingException.getMessage());
                } catch (Exception e) {
                    LOGGER.error(
                        "create server file hard link fail, "
                            + "please let qbittorrent and ikaros instance in the same file system",
                        e);
                }
            }
        } else {
            // 是文件，则直接上传文件到服务器目录
            String postfix = FileUtils.parseFilePostfix(torrentContentPath);
            try {
                String uploadFilePath = FileUtils.buildAppUploadFilePath(postfix);
                File uploadFile = new File(uploadFilePath);
                if (uploadFile.exists()) {
                    return;
                }
                Files.createLink(uploadFile.toPath(), torrentContentFile.toPath());
                LOGGER.debug("copy server file hard link success, link={}, existing={}",
                    uploadFilePath, torrentContentPath);
                String fileName = FileUtils.parseFileName(torrentContentPath);

                FileEntity fileEntity = fileService.create(new FileEntity()
                    .setPlace(FilePlace.LOCAL)
                    .setUrl(uploadFilePath.replace(SystemVarUtils.getCurrentAppDirPath(), ""))
                    .setName(fileName)
                    .setType(FileUtils.parseTypeByPostfix(postfix)));

                seasonService.updateEpisodeUrlByFileEntity(fileEntity);
            } catch (RegexMatchingException regexMatchingException) {
                LOGGER.warn("regex matching fail, msg: {}", regexMatchingException.getMessage());
            } catch (Exception e) {
                LOGGER.error(
                    "create server file hard link fail, "
                        + "please let qbittorrent and ikaros instance in the same file system",
                    e);
            }
        }
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

}
