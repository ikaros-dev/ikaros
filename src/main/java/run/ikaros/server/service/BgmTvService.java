package run.ikaros.server.service;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.constants.IkarosCons;
import run.ikaros.server.model.bgmtv.BgmTvEpisode;
import run.ikaros.server.model.bgmtv.BgmTvEpisodeType;
import run.ikaros.server.model.bgmtv.BgmTvPagingData;
import run.ikaros.server.model.bgmtv.BgmTvSubject;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.model.option.ThirdPartyOptionModel;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author guohao
 * @date 2022/10/20
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class BgmTvService implements InitializingBean {
    private final OptionService optionService;
    private final RestTemplate restTemplate;
    private final FileService fileService;
    private final HttpHeaders headers = new HttpHeaders();
    private ThirdPartyOptionModel thirdPartyOM = new ThirdPartyOptionModel();

    public BgmTvService(OptionService optionService, RestTemplate restTemplate,
                        FileService fileService) {
        this.optionService = optionService;
        this.restTemplate = restTemplate;
        this.fileService = fileService;
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
        thirdPartyOM = optionService.findOptionModel(thirdPartyOM);
    }

    @Retryable
    public BgmTvSubject getSubject(Long subjectId) {
        AssertUtils.isPositive(subjectId, "'subjectId' must be positive");
        // https://api.bgm.tv/v0/subjects/373267
        String bgmTvSubjectsUrl = thirdPartyOM.getBangumiApiBase()
            + thirdPartyOM.getBangumiApiSubjects() + "/" + subjectId;

        ResponseEntity<BgmTvSubject> responseEntity = restTemplate
            .exchange(bgmTvSubjectsUrl, HttpMethod.GET, new HttpEntity<>(null, headers),
                BgmTvSubject.class);

        return responseEntity.getBody();
    }

    @Retryable
    public List<BgmTvEpisode> getEpisodesBySubjectId(Long subjectId,
                                                     BgmTvEpisodeType bgmTvEpisodeType) {
        AssertUtils.isPositive(subjectId, "'subjectId' must be positive");
        // https://api.bgm.tv/v0/episodes?subject_id=373267&type=0&limit=100&offset=0
        String url = thirdPartyOM.getBangumiApiBase()
            + thirdPartyOM.getBangumiApiEpisodes() + "?subject_id=" + subjectId
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

    @Retryable
    public FileEntity downloadCover(String url) {
        AssertUtils.notBlank(url, "'url' must not be blank");
        ResponseEntity<byte[]> responseEntity =
            restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
        final byte[] bytes = responseEntity.getBody();
        String originalFileName = url.substring(url.lastIndexOf("/") + 1);
        return fileService.upload(originalFileName, bytes);
    }

}
