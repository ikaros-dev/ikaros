package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.MikanEpUrlBgmTvSubjectIdService;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.RssService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.core.service.TaskService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.MikanEpUrlBgmTvSubjectIdEntity;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.exceptions.RegexMatchingException;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.init.option.ThirdPartyPresetOption;
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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final MikanEpUrlBgmTvSubjectIdService mikanEpUrlBgmTvSubjectIdService;
    private final QbittorrentClient qbittorrentClient;
    private Set<String> hasHandledTorrentHashSet = new HashSet<>();

    public TaskServiceImpl(RssService rssService, OptionService optionService,
                           MikanService mikanService, BgmTvService bgmTvService,
                           FileService fileService, SeasonService seasonService,
                           MikanEpUrlBgmTvSubjectIdService mikanEpUrlBgmTvSubjectIdService,
                           QbittorrentClient qbittorrentClient) {
        this.rssService = rssService;
        this.optionService = optionService;
        this.mikanService = mikanService;
        this.bgmTvService = bgmTvService;
        this.fileService = fileService;
        this.seasonService = seasonService;
        this.mikanEpUrlBgmTvSubjectIdService = mikanEpUrlBgmTvSubjectIdService;
        this.qbittorrentClient = qbittorrentClient;
    }

    @Override
    public void pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents() {
        LOGGER.info("exec task: pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents");
        ThirdPartyPresetOption thirdPartyPresetOption =
            optionService.findPresetOption(new ThirdPartyPresetOption());
        String mikanMySubscribeRssUrl = thirdPartyPresetOption.getMikanMySubscribeRssUrl();
        LOGGER.info("start parse mikan my subscribe rss url from db");
        List<MikanRssItem> mikanRssItemList =
            rssService.parseMikanMySubscribeRss(mikanMySubscribeRssUrl);
        LOGGER.info("parse mikan my subscribe rss url to mikan rss item list ");

        for (MikanRssItem mikanRssItem : mikanRssItemList) {
            try {
                LOGGER.info("start for each mikan rss item list for item title: {}",
                    mikanRssItem.getTitle());

                qbittorrentClient.addTorrentFromUrl(mikanRssItem.getTorrentUrl());
                LOGGER.info("add to qbittorrent for torrent torrentUrlList");

                String episodePageUrl = mikanRssItem.getEpisodePageUrl();
                Long bgmtvSubjectId = null;
                if (mikanEpUrlBgmTvSubjectIdService.existsByMikanEpisodeUrl(episodePageUrl)) {
                    MikanEpUrlBgmTvSubjectIdEntity mikanEpUrlBgmTvSubjectIdEntity =
                        mikanEpUrlBgmTvSubjectIdService.findByMikanEpisodeUrl(episodePageUrl);
                    AssertUtils.notNull(mikanEpUrlBgmTvSubjectIdEntity,
                        "mikanEpUrlBgmTvSubjectIdEntity");
                    bgmtvSubjectId = mikanEpUrlBgmTvSubjectIdEntity.getBgmtvSubjectId();
                    LOGGER.debug("find exist relation for mikan ep url and bgmtv subject id, "
                        + "episodePageUrl={}, bgmtvSubjectId={}", episodePageUrl, bgmtvSubjectId);
                } else {
                    // 这里由于bgmtv的旧查询API可用，直接解析标题，作为关键词查询对应的条目ID
                    // 标题例子：[Lilith-Raws] 孤独摇滚！ / Bocchi the Rock!
                    // - 06 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]
                    String title = mikanRssItem.getTitle();
                    if (StringUtils.isBlank(title)) {
                        LOGGER.warn("title is blank, skip this mikan rss item: {}",
                            JsonUtils.obj2Json(mikanRssItem));
                        continue;
                    }
                    String enName = RegexUtils.getMatchingEnglishStrWithoutTag(title);
                    String chName = RegexUtils.getMatchingChineseStrWithoutTag(title);

                    Optional<BgmTvSubject> enNameSubOptional = Optional.empty();
                    if (StringUtils.isNotBlank(enName)) {
                        enNameSubOptional =
                            bgmTvService.searchSubject(enName, BgmTvSubjectType.ANIME)
                                .stream().findFirst();
                    }
                    if (enNameSubOptional.isPresent()) {
                        bgmtvSubjectId = Long.valueOf(enNameSubOptional.get().getId());
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
                            bgmtvSubjectId = Long.valueOf(chNameSubOptional.get().getId());
                            LOGGER.info("search bgmtv subject by chinese name success,"
                                + "bgmtvSubjectId={}, chName={}", bgmtvSubjectId, chName);
                        } else {
                            // 如果英文中文都搜不到，最后通过密柑剧集URL请求密柑页面获取对应的 bgmtv条目ID
                            String animePageUrl =
                                mikanService.getAnimePageUrlByEpisodePageUrl(episodePageUrl);
                            String bgmTvSubjectPageUrl =
                                mikanService.getBgmTvSubjectPageUrlByAnimePageUrl(animePageUrl);
                            String bgmTvSubjectIdStr =
                                bgmTvSubjectPageUrl.substring(
                                    bgmTvSubjectPageUrl.lastIndexOf("/") + 1);
                            bgmtvSubjectId = Long.valueOf(bgmTvSubjectIdStr);
                            LOGGER.info(
                                "search bgmtv subject by mikan episode to anime page success,"
                                    + "bgmtvSubjectId={}", bgmtvSubjectId);
                        }
                    }

                    mikanEpUrlBgmTvSubjectIdService.save(
                        new MikanEpUrlBgmTvSubjectIdEntity(episodePageUrl, bgmtvSubjectId));
                    LOGGER.debug("save new relation for mikan ep url and bgmtv subject id, "
                        + "episodePageUrl={}, bgmtvSubjectId={}", episodePageUrl, bgmtvSubjectId);
                }

                try {
                    bgmTvService.reqBgmtvSubject(bgmtvSubjectId);
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
        // 这个任务在5分钟一次的定时任务中也会执行
        // @see ScheduledTaskConfig#fiveMinuteOnceTask()
        qbittorrentClient.tryToResumeAllMissingFilesErroredTorrents();

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
            // PS: 目前根据RSS订阅的torrent只有单文件，也就是这个路径目前就是绝对路径
            // todo 相对路径处理

            // 创建两个文件硬链接：服务器上传目录 和 Jellyfin目录
            createServerFileHardLink(qbTorrentInfo.getName(), contentPath);
            createJellyfinFileHardLink(qbTorrentInfo.getName(), contentPath);

            hasHandledTorrentHashSet.add(hash);
        }


    }

    private void createServerFileHardLink(String torrentName, String downloadFilePath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(downloadFilePath, "downloadFilePath");
        String postfix = FileUtils.parseFilePostfix(downloadFilePath);
        String uploadFilePath = FileUtils.buildAppUploadFilePath(postfix);
        File uploadFile = new File(uploadFilePath);

        // qbittorrent download path => ikaros app download path
        // /downloads/xxx => /opt/ikaros/downloads/xxx
        downloadFilePath = addPrefixForQbittorrentDownloadPath(downloadFilePath);

        try {
            if (uploadFile.exists()) {
                return;
            }
            Files.createLink(uploadFile.toPath(), new File(downloadFilePath).toPath());
            LOGGER.debug("copy server file hard link success, link={}, existing={}",
                uploadFilePath, downloadFilePath);
            String fileName = FileUtils.parseFileName(downloadFilePath);

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

    private static String addPrefixForQbittorrentDownloadPath(String downloadFilePath) {
        return SystemVarUtils.getCurrentAppDirPath()
            + (downloadFilePath.startsWith(String.valueOf(File.separatorChar))
            ? downloadFilePath : (File.separatorChar + downloadFilePath));
    }

    private void createJellyfinFileHardLink(String torrentName, String downloadFilePath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(downloadFilePath, "downloadFilePath");
        downloadFilePath = addPrefixForQbittorrentDownloadPath(downloadFilePath);

        final ThirdPartyPresetOption thirdPartyPresetOption =
            optionService.findPresetOption(new ThirdPartyPresetOption());

        String matchingEnglishStr = null;
        try {
            matchingEnglishStr = RegexUtils.getMatchingEnglishStrWithoutTag(torrentName);
        } catch (RegexMatchingException regexMatchingException) {
            LOGGER.warn("match fail", regexMatchingException);
        }

        String dirName = matchingEnglishStr;
        if (StringUtils.isBlank(matchingEnglishStr)) {
            dirName = RegexUtils.getMatchingChineseStrWithoutTag(torrentName);
        }

        String jellyfinMediaDirPath = thirdPartyPresetOption.getJellyfinMediaDirPath()
            + File.separatorChar
            + (StringUtils.isBlank(dirName)
            ? torrentName
            .replaceAll(RegexConst.FILE_NAME_TAG, "")
            .replaceAll(RegexConst.FILE_POSTFIX, "")
            : dirName);

        // jellyfin media path => ikaros app media path
        // /media/xxx => /opt/ikaros/media/xxx
        jellyfinMediaDirPath = SystemVarUtils.getCurrentAppDirPath()
            + (jellyfinMediaDirPath.startsWith("/")
            ? jellyfinMediaDirPath : (File.separatorChar + jellyfinMediaDirPath));

        File jellyfinMediaDir = new File(jellyfinMediaDirPath);
        if (!jellyfinMediaDir.exists()) {
            jellyfinMediaDir.mkdirs();
        }
        String fileName = FileUtils.parseFileName(downloadFilePath);
        Long seq = RegexUtils.getFileNameTagEpSeq(fileName);
        fileName = "S1E" + seq + "-" + fileName;
        String jellyfinFilePath = jellyfinMediaDirPath + File.separatorChar + fileName;
        File jellyfinFile = new File(jellyfinFilePath);

        try {
            if (jellyfinFile.exists()) {
                return;
            }
            Files.createLink(jellyfinFile.toPath(), new File(downloadFilePath).toPath());
            LOGGER.debug("copy jellyfin file hard link success, link={}, existing={}",
                jellyfinFilePath, downloadFilePath);

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
