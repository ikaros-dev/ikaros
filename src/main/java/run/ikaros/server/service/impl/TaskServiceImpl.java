package run.ikaros.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.bt.qbittorrent.QbittorrentClient;
import run.ikaros.server.bt.qbittorrent.enums.QbTorrentInfoFilter;
import run.ikaros.server.bt.qbittorrent.model.QbTorrentInfo;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.init.option.ThirdPartyPresetOption;
import run.ikaros.server.rss.mikan.model.MikanRssItem;
import run.ikaros.server.service.BgmTvService;
import run.ikaros.server.service.FileService;
import run.ikaros.server.service.MikanService;
import run.ikaros.server.service.OptionService;
import run.ikaros.server.service.RssService;
import run.ikaros.server.service.SeasonService;
import run.ikaros.server.service.TaskService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.FileUtils;
import run.ikaros.server.utils.RegexUtils;
import run.ikaros.server.utils.SystemVarUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
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
    private final QbittorrentClient qbittorrentClient;
    private Set<String> hasHandledTorrentHashSet = new HashSet<>();

    public TaskServiceImpl(RssService rssService, OptionService optionService,
                           MikanService mikanService, BgmTvService bgmTvService,
                           FileService fileService, SeasonService seasonService,
                           QbittorrentClient qbittorrentClient) {
        this.rssService = rssService;
        this.optionService = optionService;
        this.mikanService = mikanService;
        this.bgmTvService = bgmTvService;
        this.fileService = fileService;
        this.seasonService = seasonService;
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

            String animePageUrl =
                mikanService.getAnimePageUrlByEpisodePageUrl(episodePageUrl);
            String bgmTvSubjectPageUrl =
                mikanService.getBgmTvSubjectPageUrlByAnimePageUrl(animePageUrl);

            String bgmTvSubjectId =
                bgmTvSubjectPageUrl.substring(bgmTvSubjectPageUrl.lastIndexOf("/") + 1);

            bgmTvService.reqBgmtvSubject(Long.valueOf(bgmTvSubjectId));
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

    private void createServerFileHardLink(String torrentName, String filePath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(filePath, "filePath");
        String postfix = FileUtils.parseFilePostfix(filePath);
        String uploadFilePath = FileUtils.buildAppUploadFilePath(postfix);
        File uploadFile = new File(uploadFilePath);
        try {
            if (uploadFile.exists()) {
                return;
            }
//            Files.createLink(Path.of(uploadFile.toURI()), Path.of(new File(filePath).toURI()));
//            LOGGER.debug("create file hard link success, link={}, exist={}",
//                uploadFilePath, filePath);
            Files.copy(new File(filePath).toPath(), uploadFile.toPath());
            LOGGER.debug("copy file success, source={}, target={}", filePath, uploadFilePath);
            String fileName = FileUtils.parseFileName(filePath);

            FileEntity fileEntity = fileService.save(new FileEntity()
                .setPlace(FilePlace.LOCAL)
                .setUrl(uploadFilePath.replace(SystemVarUtils.getCurrentAppDirPath(), ""))
                .setName(fileName)
                .setType(FileUtils.parseTypeByPostfix(postfix)));

            seasonService.updateEpisodeUrlByFileEntity(fileEntity);
        } catch (Exception e) {
            LOGGER.error(
                "create file hard link fail, "
                    + "please let qbittorrent and ikaros instance in the same file system",
                e);
        }
    }

    private void createJellyfinFileHardLink(String torrentName, String filePath) {
        AssertUtils.notBlank(torrentName, "torrentName");
        AssertUtils.notBlank(filePath, "filePath");
        ThirdPartyPresetOption thirdPartyPresetOption =
            optionService.findPresetOption(new ThirdPartyPresetOption());
        String jellyfinMediaDirPath = thirdPartyPresetOption.getJellyfinMediaDirPath()
            + File.separatorChar + RegexUtils.getMatchingEnglishStr(
                torrentName.replaceAll(RegexConst.FILE_NAME_TAG, ""));
        jellyfinMediaDirPath = jellyfinMediaDirPath.replace("AAC", "");
        jellyfinMediaDirPath = jellyfinMediaDirPath.replace("AVCmp", "");
        File jellyfinMediaDir = new File(jellyfinMediaDirPath);
        if (!jellyfinMediaDir.exists()) {
            jellyfinMediaDir.mkdirs();
        }
        String fileName = FileUtils.parseFileName(filePath);
        Long seq = RegexUtils.getFileNameTagEpSeq(fileName);
        fileName = "S1E" + seq + "-" + fileName;
        String jellyfinFilePath = jellyfinMediaDirPath + File.separatorChar + fileName;
        File jellyfinFile = new File(jellyfinFilePath);

        try {
            if (jellyfinFile.exists()) {
                return;
            }
            Files.copy(new File(filePath).toPath(), jellyfinFile.toPath());
            LOGGER.debug("copy file success, source={}, target={}", filePath, jellyfinFile);
//            Files.createLink(Path.of(jellyfinFile.toURI()), Path.of(new File(filePath).toURI()));
//            LOGGER.debug("create file hard link success, link={}, exist={}",
//                jellyfinFilePath, filePath);

        } catch (Exception e) {
            LOGGER.error(
                "create file hard link fail, please let qbittorrent and ikaros "
                    + "and jellyfin instance in the same file system",
                e);
        }
    }

}
