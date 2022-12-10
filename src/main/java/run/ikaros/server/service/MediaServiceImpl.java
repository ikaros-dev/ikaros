package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.MediaService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.BaseEntity;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.utils.FileUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.SystemVarUtils;
import run.ikaros.server.utils.TimeUtils;
import run.ikaros.server.utils.XmlUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class MediaServiceImpl implements MediaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);
    private final AnimeService animeService;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;
    private final FileService fileService;

    public MediaServiceImpl(AnimeService animeService, SeasonService seasonService,
                            EpisodeService episodeService, FileService fileService) {
        this.animeService = animeService;
        this.seasonService = seasonService;
        this.episodeService = episodeService;
        this.fileService = fileService;
    }

    @Override
    public void generateMediaDir() {
        List<AnimeEntity> animeEntityList
            = animeService
            .listAll().stream()
            .filter(BaseEntity::getStatus)
            .toList();
        for (AnimeEntity animeEntity : animeEntityList) {
            generateMediaDirBySingleAnimeEntity(animeEntity);
        }
    }


    /**
     * @see <a href="https://jellyfin.org/docs/general/server/media/shows">jellyfin tvshow</a>
     */
    private void generateMediaDirBySingleAnimeEntity(@Nonnull AnimeEntity animeEntity) {
        Date airTime = animeEntity.getAirTime();
        LocalDateTime localDateTime = null;
        if (airTime != null) {
            localDateTime = TimeUtils.date2LocalDataTime(airTime);
        }

        // generate anime dir
        String animeDirName = animeEntity.getTitleCn() + " - " + animeEntity.getTitle();
        if (localDateTime != null) {
            animeDirName += " (" + localDateTime.getYear() + "-" + localDateTime.getMonthValue()
                + "-" + localDateTime.getDayOfMonth() + ")";
        }
        final String animeDirPath = SystemVarUtils.getCurrentAppMediaDirPath()
            + File.separator + animeDirName;
        File animeDir = new File(animeDirPath);
        if (!animeDir.exists()) {
            animeDir.mkdirs();
            LOGGER.info("mkdirs media anime dir: {}", animeDirPath);
        }

        // generate anime tvshow.nfo
        String tvshowNfoFilePath = animeDirPath + File.separator + "tvshow.nfo";
        File tvshowNfoFile = new File(tvshowNfoFilePath);
        if (!tvshowNfoFile.exists()) {
            XmlUtils.generateJellyfinTvShowNfoXml(tvshowNfoFilePath,
                animeEntity.getOverview(), animeEntity.getTitleCn(),
                animeEntity.getTitle(),
                String.valueOf(animeEntity.getBgmtvId()));
        }

        // generate anime poster.jpg
        String coverUrl = animeEntity.getCoverUrl();
        String coverFilePath = SystemVarUtils.getCurrentAppDirPath()
            + (coverUrl.startsWith(File.separator) ? coverUrl : File.separator + coverUrl);
        File coverFile = new File(coverFilePath);
        if (coverFile.exists()) {
            String postfix = FileUtils.parseFilePostfix(coverFilePath);
            String posterFilePath = animeDirPath
                + File.separator
                + "poster"
                + (StringUtils.isBlank(postfix) ? ".jpg" :
                postfix.startsWith(".") ? postfix : "." + postfix);
            File posterFile = new File(posterFilePath);
            if (!posterFile.exists()) {
                try {
                    Files.createLink(posterFile.toPath(), coverFile.toPath());
                    LOGGER.info(
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
                "cover file not exist, skip create poster.jpg for animeDirPath: {}",
                animeDirPath);
        }

        // find first season episodes
        List<SeasonEntity> seasonEntityList = seasonService.findByAnimeId(animeEntity.getId());
        if (seasonEntityList.isEmpty()) {
            LOGGER.warn("not season found for anime title={}", animeEntity.getTitle());
            return;
        }
        for (SeasonEntity seasonEntity : seasonEntityList) {
            generateMediaDirBySingleSeason(animeDirPath, seasonEntity, animeEntity.getBgmtvId());
        }
    }

    private void generateMediaDirBySingleSeason(@Nonnull String animeDirPath,
                                                @Nonnull SeasonEntity seasonEntity,
                                                Long bgmtvId) {
        SeasonType type = seasonEntity.getType();
        String seasonDirName = "Season ";
        int seasonNum = type.getOrder();
        seasonDirName += seasonNum;

        // generate season dir
        final String seasonDirPath = animeDirPath + File.separator + seasonDirName;
        File seasonDir = new File(seasonDirPath);
        if (!seasonDir.exists()) {
            seasonDir.mkdirs();
            LOGGER.info("mkdirs media season dir: {}", seasonDirPath);
        }

        // link episode file
        List<EpisodeEntity> episodeEntityList = episodeService.findBySeasonId(seasonEntity.getId());
        if (episodeEntityList.isEmpty()) {
            LOGGER.warn("not episode found for season title={}", seasonEntity.getTitle());
            return;
        }

        for (EpisodeEntity episodeEntity : episodeEntityList) {
            Long seq = episodeEntity.getSeq();
            // final String prefix = "S" + seasonNum + "E" + seq + " - ";
            String url = episodeEntity.getUrl();
            if (StringUtils.isBlank(url)) {
                // LOGGER.warn(
                //     "current episode not relation file url, season title={} episode title={}",
                //     seasonEntity.getTitle(), episodeEntity.getTitle());
                continue;
            }

            url = url.replace("/", File.separator);
            String uploadEpFilePath = SystemVarUtils.getCurrentAppDirPath()
                + File.separator + url;

            File uploadedEpFile = new File(uploadEpFilePath);
            if (!uploadedEpFile.exists()) {
                LOGGER.warn(
                    "current uploaded episode file not exist, ep file path={} "
                        + ", season title={} episode title={}",
                    uploadEpFilePath, seasonEntity.getTitle(), episodeEntity.getTitle());
                continue;
            }
            Optional<FileEntity> fileEntityOptional = fileService.findByUrl(url);
            String fileName;
            if (fileEntityOptional.isPresent()) {
                fileName = fileEntityOptional.get().getName();
            } else {
                fileName = FileUtils.parseFileName(uploadEpFilePath);
            }


            String mediaEpFilePath = seasonDirPath + File.separator + fileName;
            File mediaEpFile = new File(mediaEpFilePath);

            if (!mediaEpFile.exists()) {
                try {
                    Files.createLink(mediaEpFile.toPath(), uploadedEpFile.toPath());
                    LOGGER.info(
                        "create media episode hard link success, link={}, existing={}",
                        mediaEpFilePath, uploadEpFilePath);
                } catch (IOException e) {
                    LOGGER.error(
                        "create media episode hard link fail, link={}, existing={}",
                        mediaEpFilePath, uploadEpFilePath);
                }
            } else {
                // LOGGER.debug("file has exists for media episode file path={}", mediaEpFile);
            }

            // generate episode nfo
            String episodeNfoFilePath = seasonDirPath + File.separator + fileName.replaceAll(
                RegexConst.FILE_POSTFIX, "") + ".nfo";
            File episodeNfoFile = new File(episodeNfoFilePath);
            if (!episodeNfoFile.exists()) {
                String episodePlot =
                    StringUtils.isNotBlank(episodeEntity.getTitleCn()) ? episodeEntity.getTitleCn()
                        : episodeEntity.getTitle();
                XmlUtils.generateJellyfinEpisodeNfoXml(episodeNfoFilePath,
                    episodeEntity.getOverview(), episodePlot, String.valueOf(seasonNum),
                    String.valueOf(seq), String.valueOf(bgmtvId));
            }
        }
    }


}
