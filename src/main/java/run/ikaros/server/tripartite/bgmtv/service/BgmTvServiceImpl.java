package run.ikaros.server.tripartite.bgmtv.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.core.tripartite.bgmtv.constants.BgmTvConst;
import run.ikaros.server.core.tripartite.bgmtv.repository.BgmTvRepository;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.model.dto.OptionBgmTvDTO;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvEpisode;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvEpisodeType;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvImages;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvTag;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvUserInfo;
import run.ikaros.server.tripartite.bgmtv.repository.BgmTvRepositoryImpl;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.DateUtils;
import run.ikaros.server.utils.StringUtils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class BgmTvServiceImpl implements BgmTvService, InitializingBean {
    private final BgmTvRepository bgmTvRepository;

    private final FileService fileService;
    private final AnimeService animeService;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;
    private final OptionService optionService;

    public BgmTvServiceImpl(BgmTvRepositoryImpl bgmTvRepository, FileService fileService,
                            AnimeService animeService, SeasonService seasonService,
                            EpisodeService episodeService, OptionService optionService) {
        this.bgmTvRepository = bgmTvRepository;
        this.fileService = fileService;
        this.animeService = animeService;
        this.seasonService = seasonService;
        this.episodeService = episodeService;
        this.optionService = optionService;
    }


    @Override
    public void setRestTemplate(@Nonnull RestTemplate restTemplate) {
        AssertUtils.notNull(restTemplate, "restTemplate");
        bgmTvRepository.setRestTemplate(restTemplate);
    }

    @Nonnull
    @Override
    public List<BgmTvSubject> searchSubject(@Nonnull String keyword,
                                            @Nonnull BgmTvSubjectType type) {
        AssertUtils.notBlank(keyword, "keyword");
        AssertUtils.notNull(type, "bgmtv subject type");
        return bgmTvRepository.searchSubjectWithOldApi(keyword, type);
    }

    @Nullable
    @Override
    public BgmTvSubject getSubject(@Nonnull Long subjectId) {
        AssertUtils.isPositive(subjectId, "'subjectId' must be positive");
        return bgmTvRepository.getSubject(subjectId);
    }

    @Nonnull
    @Override
    public FileEntity downloadCover(@Nonnull String url) {
        AssertUtils.notBlank(url, "url");
        final byte[] bytes = bgmTvRepository.downloadCover(url);
        String originalFileName = url.substring(url.lastIndexOf("/") + 1);
        return fileService.upload(originalFileName, bytes);
    }

    @Nullable
    @Override
    public AnimeDTO reqBgmtvSubject(@Nonnull Long subjectId) {
        AssertUtils.isPositive(subjectId, "subjectId");
        // 直接返回已经存在的番剧数据
        AnimeEntity animeEntityExample = new AnimeEntity();
        animeEntityExample.setBgmtvId(subjectId).setStatus(null);
        List<AnimeEntity> animeEntityList = animeService.listAll(Example.of(animeEntityExample));

        if (!animeEntityList.isEmpty()) {
            AnimeEntity existAnimeEntity = animeEntityList.get(0);
            if (!existAnimeEntity.getStatus()) {
                existAnimeEntity.setStatus(true);
                existAnimeEntity = animeService.save(existAnimeEntity);
            }
            Long id = existAnimeEntity.getId();
            AnimeDTO animeDTO = animeService.findAnimeDTOById(id);
            log.debug("anime exist, return this anime, subjectId={}, animeId={}", subjectId, id);
            return animeDTO;
        } else {
            // 创建新的动漫对象
            log.debug("anime not exist, will  create a new, subjectId={}", subjectId);

            // 获取动漫信息
            BgmTvSubject bgmTvSubject = getSubject(subjectId);
            if (bgmTvSubject == null) {
                log.warn("request bgmtv fail, response null subject");
                return null;
            }

            AnimeEntity animeEntity = new AnimeEntity();
            animeEntity
                .setBgmtvId(subjectId)
                .setPlatform(bgmTvSubject.getPlatform())
                .setOverview(bgmTvSubject.getSummary())
                .setTitle(bgmTvSubject.getName())
                .setTitleCn(bgmTvSubject.getNameCn());

            BgmTvImages images = bgmTvSubject.getImages();
            String coverUrl = images.getLarge();
            if (StringUtils.isBlank(coverUrl)) {
                coverUrl = images.getGrid();
            }
            if (StringUtils.isBlank(coverUrl)) {
                coverUrl = images.getMedium();
            }
            if (StringUtils.isBlank(coverUrl)) {
                coverUrl = images.getSmall();
            }
            if (StringUtils.isBlank(coverUrl)) {
                coverUrl = images.getCommon();
            }

            if (StringUtils.isNotBlank(coverUrl)) {
                FileEntity coverFileEntity = downloadCover(coverUrl);
                animeEntity.setCoverUrl(coverFileEntity.getUrl());
            }

            Date date = null;
            if (bgmTvSubject.getDate() != null) {
                date = DateUtils.parseDateStr(bgmTvSubject.getDate(), BgmTvConst.DATE_PATTERN);
            }
            if (date != null) {
                animeEntity.setAirTime(date);
            }

            if (StringUtils.isBlank(animeEntity.getTitle())) {
                animeEntity.setTitleCn(animeEntity.getTitle());
            }

            animeEntity = animeService.save(animeEntity);

            for (BgmTvTag bgmTvTag : bgmTvSubject.getTags()) {
                // todo save tag info
            }

            // 获取动漫剧集信息
            List<BgmTvEpisode> bgmTvEpisodes =
                bgmTvRepository.findEpisodesBySubjectId(subjectId, BgmTvEpisodeType.POSITIVE,
                    null, null);

            SeasonEntity seasonEntity = new SeasonEntity()
                .setType(SeasonType.FIRST)
                .setAnimeId(animeEntity.getId())
                .setTitle(animeEntity.getTitle())
                .setTitleCn(animeEntity.getTitleCn())
                .setOverview(animeEntity.getOverview());
            seasonEntity = seasonService.save(seasonEntity);

            for (BgmTvEpisode bgmTvEpisode : bgmTvEpisodes) {
                EpisodeEntity episodeEntity = new EpisodeEntity()
                    .setSeasonId(seasonEntity.getId())
                    .setSeq(bgmTvEpisode.getSort().longValue())
                    .setTitleCn(bgmTvEpisode.getNameCn())
                    .setTitle(bgmTvEpisode.getName())
                    .setOverview(bgmTvEpisode.getDesc())
                    .setDuration(bgmTvEpisode.getDurationSeconds().longValue());

                String airDateStr = bgmTvEpisode.getAirDate();
                if (StringUtils.isNotBlank(airDateStr)) {
                    Date airDate =
                        DateUtils.parseDateStr(airDateStr, BgmTvConst.DATE_PATTERN);
                    episodeEntity.setAirTime(airDate);
                }
                episodeEntity = episodeService.save(episodeEntity);
            }

            return animeService.findAnimeDTOById(animeEntity.getId());
        }
    }

    @Override
    public void refreshHttpHeaders(@Nullable String accessToken) {
        log.debug("refresh http headers");
        bgmTvRepository.refreshHttpHeaders(accessToken);
    }

    @Override
    public BgmTvUserInfo getMe() {
        return bgmTvRepository.getMe();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        OptionBgmTvDTO optionBgmTvDTO = optionService.getOptionBgmTvDTO();
        Boolean enableProxy = optionBgmTvDTO.getEnableProxy();
        if (enableProxy != null && enableProxy) {
            String accessToken = optionBgmTvDTO.getAccessToken();
            if (StringUtils.isNotBlank(accessToken)) {
                refreshHttpHeaders(accessToken);
            }
        }
    }
}
