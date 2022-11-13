package run.ikaros.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.bt.qbittorrent.QbittorrentClient;
import run.ikaros.server.bt.qbittorrent.enums.QbTorrentInfoFilter;
import run.ikaros.server.bt.qbittorrent.model.QbTorrentInfo;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.MikanEpUrlBgmTvSubjectIdEntity;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.exceptions.RegexMatchingException;
import run.ikaros.server.init.option.ThirdPartyPresetOption;
import run.ikaros.server.rss.mikan.model.MikanRssItem;
import run.ikaros.server.service.BgmTvService;
import run.ikaros.server.service.FileService;
import run.ikaros.server.service.MikanEpUrlBgmTvSubjectIdService;
import run.ikaros.server.service.MikanService;
import run.ikaros.server.service.OptionService;
import run.ikaros.server.service.RssService;
import run.ikaros.server.service.SeasonService;
import run.ikaros.server.service.TaskService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.FileUtils;
import run.ikaros.server.utils.RegexUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.SystemVarUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        List<MikanRssItem> mikanRssItemList =
            rssService.parseMikanMySubscribeRss(mikanMySubscribeRssUrl);

        for (MikanRssItem mikanRssItem : mikanRssItemList) {
            String episodePageUrl = mikanRssItem.getEpisodePageUrl();
            qbittorrentClient.addTorrentFromUrl(mikanRssItem.getTorrentUrl());

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
                String animePageUrl =
                    mikanService.getAnimePageUrlByEpisodePageUrl(episodePageUrl);
                String bgmTvSubjectPageUrl =
                    mikanService.getBgmTvSubjectPageUrlByAnimePageUrl(animePageUrl);

                String bgmTvSubjectIdStr =
                    bgmTvSubjectPageUrl.substring(bgmTvSubjectPageUrl.lastIndexOf("/") + 1);

                bgmtvSubjectId = Long.valueOf(bgmTvSubjectIdStr);

                mikanEpUrlBgmTvSubjectIdService.save(
                    new MikanEpUrlBgmTvSubjectIdEntity(episodePageUrl, bgmtvSubjectId));
                LOGGER.debug("save new relation for mikan ep url and bgmtv subject id, "
                    + "episodePageUrl={}, bgmtvSubjectId={}", episodePageUrl, bgmtvSubjectId);
            }

            bgmTvService.reqBgmtvSubject(bgmtvSubjectId);
        }

        // 如果新添加的种子文件状态是缺失文件，则需要再恢复下
        qbittorrentClient.getTorrentList(QbTorrentInfoFilter.ERRORED,
                qbittorrentClient.getCategory(), null, 100, null, null)
            .stream()
            .filter(qbTorrentInfo -> "missingFiles".equalsIgnoreCase(qbTorrentInfo.getState()))
            .forEach(qbTorrentInfo -> qbittorrentClient.resume(qbTorrentInfo.getHash()));

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

            FileEntity fileEntity = fileService.save(new FileEntity()
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

        final String originalTorrentName = torrentName;

        torrentName = torrentName.replaceAll(RegexConst.FILE_NAME_TAG, "");
        torrentName = torrentName.replaceAll(RegexConst.FILE_POSTFIX, "");
        torrentName = torrentName.replaceAll(RegexConst.FILE_NAME_TAG_EPISODE_SEQUENCE, "");
        torrentName = torrentName.replace("-", "");
        torrentName = torrentName.trim();
        if (StringUtils.isBlank(torrentName)) {
            torrentName = originalTorrentName.trim();
        }
        String matchingEnglishStr = null;
        try {
            matchingEnglishStr = RegexUtils.getMatchingEnglishStr(torrentName);
        } catch (RegexMatchingException regexMatchingException) {
            LOGGER.warn("match fail", regexMatchingException);
        }
        String jellyfinMediaDirPath = thirdPartyPresetOption.getJellyfinMediaDirPath()
            + File.separatorChar
            + (StringUtils.isBlank(matchingEnglishStr) ? torrentName : matchingEnglishStr);

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
