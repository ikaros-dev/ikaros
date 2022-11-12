package run.ikaros.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.constants.IkarosCons;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.init.option.ThirdPartyPresetOption;
import run.ikaros.server.model.bgmtv.BgmTvConstants;
import run.ikaros.server.model.bgmtv.BgmTvEpisode;
import run.ikaros.server.model.bgmtv.BgmTvEpisodeType;
import run.ikaros.server.model.bgmtv.BgmTvPagingData;
import run.ikaros.server.model.bgmtv.BgmTvSubject;
import run.ikaros.server.model.bgmtv.BgmTvTag;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.service.AnimeService;
import run.ikaros.server.service.BgmTvService;
import run.ikaros.server.service.EpisodeService;
import run.ikaros.server.service.FileService;
import run.ikaros.server.service.OptionService;
import run.ikaros.server.service.SeasonService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.DateUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.RegexUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.UrlUtils;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author guohao
 * @date 2022/10/20
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class BgmTvServiceImpl implements BgmTvService, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BgmTvServiceImpl.class);
    private final OptionService optionService;
    private final RestTemplate restTemplate;
    private final FileService fileService;
    private final AnimeService animeService;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;
    private final HttpHeaders headers = new HttpHeaders();
    private ThirdPartyPresetOption thirdPartyPresetOption = new ThirdPartyPresetOption();

    public BgmTvServiceImpl(OptionService optionService, RestTemplate restTemplate,
                            FileService fileService, AnimeService animeService,
                            SeasonService seasonService, EpisodeService episodeService) {
        this.optionService = optionService;
        this.restTemplate = restTemplate;
        this.fileService = fileService;
        this.animeService = animeService;
        this.seasonService = seasonService;
        this.episodeService = episodeService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 需要设置bgmTv API 要求的 User Agent, 具体请看：https://github.com/bangumi/api/blob/master/docs-raw/user%20agent.md
        // todo 目前设置成GitHub仓库地址，后续官网上线设置成官网地址
        // User-Agent格式 ikaros-dev/ikaros (https://github.com/ikaros-dev/ikaros)
        String userAgent = IkarosCons.REPO_GITHUB_NAME
            + " ( " + IkarosCons.REPO_GITHUB_URL + ")";
        headers.set(HttpHeaders.USER_AGENT, userAgent);

        // 从数据库更新三方配置
        thirdPartyPresetOption = optionService.findPresetOption(thirdPartyPresetOption);
    }

    @Retryable
    public BgmTvSubject getSubject(Long subjectId) {
        AssertUtils.isPositive(subjectId, "'subjectId' must be positive");
        // https://api.bgm.tv/v0/subjects/373267
        String bgmTvSubjectsUrl = thirdPartyPresetOption.getBangumiApiBase()
            + thirdPartyPresetOption.getBangumiApiSubjects() + "/" + subjectId;

        try {
            ResponseEntity<BgmTvSubject> responseEntity = restTemplate
                .exchange(bgmTvSubjectsUrl, HttpMethod.GET, new HttpEntity<>(null, headers),
                    BgmTvSubject.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.warn("subject not found for subjectId={}", subjectId);
            }
            return null;
        }
    }

    @Retryable
    public List<BgmTvEpisode> getEpisodesBySubjectId(Long subjectId,
                                                     BgmTvEpisodeType bgmTvEpisodeType) {
        AssertUtils.isPositive(subjectId, "'subjectId' must be positive");
        // https://api.bgm.tv/v0/episodes?subject_id=373267&type=0&limit=100&offset=0
        String url = thirdPartyPresetOption.getBangumiApiBase()
            + thirdPartyPresetOption.getBangumiApiEpisodes() + "?subject_id=" + subjectId
            + "&type=" + bgmTvEpisodeType.getCode() + "&limit=100&offset=0";

        ResponseEntity<BgmTvPagingData> responseEntity = restTemplate
            .exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers),
                BgmTvPagingData.class);
        BgmTvPagingData bgmTvPagingData = responseEntity.getBody();
        List<BgmTvEpisode> bgmTvEpisodes = new ArrayList<>();

        for (Object obj : bgmTvPagingData.getData()) {
            String json = JsonUtils.obj2Json(obj);
            BgmTvEpisode bgmTvEpisode = JsonUtils.json2obj(json, BgmTvEpisode.class);
            bgmTvEpisodes.add(bgmTvEpisode);
        }

        return bgmTvEpisodes;
    }

    @Nonnull
    @Retryable
    public FileEntity downloadCover(@Nonnull String url) {
        AssertUtils.notBlank(url, "'url' must not be blank");
        ResponseEntity<byte[]> responseEntity =
            restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
        final byte[] bytes = responseEntity.getBody();
        String originalFileName = url.substring(url.lastIndexOf("/") + 1);
        return fileService.upload(originalFileName, bytes);
    }

    @Nonnull
    @Override
    public AnimeDTO reqBgmtvSubject(@Nonnull Long subjectId) {
        AssertUtils.isPositive(subjectId, "subjectId");
        // 直接返回已经存在的番剧数据
        AnimeEntity animeEntityExample = new AnimeEntity();
        animeEntityExample.setBgmtvId(subjectId).setStatus(true);
        List<AnimeEntity> animeEntityList = animeService.listAll(Example.of(animeEntityExample));

        if (!animeEntityList.isEmpty()) {
            Long id = animeEntityList.get(0).getId();
            AnimeDTO animeDTO = animeService.findAnimeDTOById(id);
            LOGGER.debug("anime exist, return this anime, subjectId={}, animeId={}", subjectId, id);
            return animeDTO;
        } else {
            // 创建新的动漫对象
            LOGGER.debug("anime not exist, will  create a new, subjectId={}", subjectId);

            // 获取动漫信息
            BgmTvSubject bgmTvSubject = getSubject(subjectId);
            if (bgmTvSubject == null) {
                LOGGER.error("request bgmtv fail, response null subject");
                return null;
            }

            AnimeEntity animeEntity = new AnimeEntity();
            String coverUrl = bgmTvSubject.getImages().getLarge();
            FileEntity coverFileEntity = downloadCover(coverUrl);
            animeEntity
                .setBgmtvId(subjectId)
                .setCoverUrl(coverFileEntity.getUrl())
                .setAirTime(
                    DateUtils.parseDateStr(bgmTvSubject.getDate(), BgmTvConstants.DATE_PATTERN))
                .setPlatform(bgmTvSubject.getPlatform())
                .setOverview(bgmTvSubject.getSummary())
                .setTitle(bgmTvSubject.getName())
                .setTitleCn(bgmTvSubject.getNameCn());

            if (StringUtils.isBlank(animeEntity.getTitle())) {
                animeEntity.setTitleCn(animeEntity.getTitle());
            }

            animeEntity = animeService.save(animeEntity);

            for (BgmTvTag bgmTvTag : bgmTvSubject.getTags()) {
                // todo save tag info
            }

            // 获取动漫剧集信息
            List<BgmTvEpisode> bgmTvEpisodes =
                getEpisodesBySubjectId(subjectId, BgmTvEpisodeType.POSITIVE);

            SeasonEntity seasonEntity = new SeasonEntity()
                .setType(SeasonType.FIRST)
                .setAnimeId(animeEntity.getId())
                .setTitle(animeEntity.getTitle())
                .setTitleCn(animeEntity.getTitleCn())
                .setOverview(animeEntity.getOverview());
            seasonEntity = seasonService.save(seasonEntity);

            for (BgmTvEpisode bgmTvEpisode : bgmTvEpisodes) {
                Date airDate =
                    DateUtils.parseDateStr(bgmTvEpisode.getAirDate(), BgmTvConstants.DATE_PATTERN);
                EpisodeEntity episodeEntity = new EpisodeEntity()
                    .setSeasonId(seasonEntity.getId())
                    .setSeq(bgmTvEpisode.getSort().longValue())
                    .setTitleCn(bgmTvEpisode.getNameCn())
                    .setTitle(bgmTvEpisode.getName())
                    .setAirTime(airDate)
                    .setOverview(bgmTvEpisode.getDesc())
                    .setDuration(bgmTvEpisode.getDurationSeconds().longValue());
                episodeEntity = episodeService.save(episodeEntity);
            }

            return animeService.findAnimeDTOById(animeEntity.getId());
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public BgmTvSubject findSubjectByQueryStr(@Nonnull String queryStr) {
        AssertUtils.notBlank(queryStr, "queryStr");
        //  curl -X 'GET' \
        //  'https://api.bgm.tv/search/subject/Do%20It%20Yourself?type=2&responseGroup=small' \
        //  -H 'accept: application/json'
        final String url = thirdPartyPresetOption.getBangumiApiBase()
            + thirdPartyPresetOption.getBangumiApiSearchSubject()
            + "/" + UrlUtils.encode(queryStr);


        byte[] bytes = restTemplate.getForObject(url, byte[].class);

        String content =
            new String(bytes, StandardCharsets.UTF_8);
        if (content.startsWith("<!DOCTYPE html>")) {
            throw new RuntimeIkarosException("you can not search frequently");
        }

        HashMap hashMap = JsonUtils.json2obj(content, HashMap.class);
        if (hashMap == null || ((Long) hashMap.get("results")) <= 0) {
            LOGGER.warn("not found subject for query str: {}", queryStr);
            return null;
        }

        Object list = hashMap.get("list");
        TypeReference<BgmTvSubject[]> typeReference = new TypeReference<>() {
        };
        BgmTvSubject[] bgmTvSubjects = JsonUtils.obj2Arr(list, typeReference);
        for (BgmTvSubject bgmTvSubject : bgmTvSubjects) {
            if (bgmTvSubject == null) {
                continue;
            }
            String name = bgmTvSubject.getName();
            if (name.contains(queryStr)) {
                return bgmTvSubject;
            }
        }

        return bgmTvSubjects[0];
    }

    @Nonnull
    @Override
    public Long pullMetadataByTitle(@Nonnull String title) {
        AssertUtils.notBlank(title, "title");

        // [SweetSub&LoliHouse] 手工少女!! / Do It Yourself!!
        // - 06 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]
        // 去掉所有中括号，这个属于 Tag
        title = title.replaceAll(RegexConst.FILE_NAME_TAG, "");
        // 获取英文(罗马音), 根据罗马音模糊查询
        String englishTitle = RegexUtils.getMatchingEnglishStr(title);


        return null;
    }
}
