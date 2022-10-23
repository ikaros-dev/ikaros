package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.JacksonConverter;
import cn.liguohao.ikaros.common.constants.IkarosConstants;
import cn.liguohao.ikaros.model.bgmtv.BgmTvEpisode;
import cn.liguohao.ikaros.model.bgmtv.BgmTvEpisodeType;
import cn.liguohao.ikaros.model.bgmtv.BgmTvPagingData;
import cn.liguohao.ikaros.model.bgmtv.BgmTvSubject;
import cn.liguohao.ikaros.model.entity.FileEntity;
import cn.liguohao.ikaros.model.option.ThirdPartyOptionModel;
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
        String userAgent = IkarosConstants.REPO_GITHUB_NAME
            + " ( " + IkarosConstants.REPO_GITHUB_URL + ")";
        headers.set(HttpHeaders.USER_AGENT, userAgent);

        // 从数据库更新三方配置
        thirdPartyOM = optionService.findOptionModel(thirdPartyOM);
    }

    @Retryable
    public BgmTvSubject getSubject(Long subjectId) {
        Assert.isPositive(subjectId, "'subjectId' must be positive");
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
        Assert.isPositive(subjectId, "'subjectId' must be positive");
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
            String json = JacksonConverter.obj2Json(obj);
            BgmTvEpisode bgmTvEpisode = JacksonConverter.json2obj(json, BgmTvEpisode.class);
            bgmTvEpisodes.add(bgmTvEpisode);
        }

        return bgmTvEpisodes;
    }

    @Retryable
    public FileEntity downloadCover(String url) {
        Assert.notBlank(url, "'url' must not be blank");
        ResponseEntity<byte[]> responseEntity =
            restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
        final byte[] bytes = responseEntity.getBody();
        String originalFileName = url.substring(url.lastIndexOf("/") + 1);
        return fileService.upload(originalFileName, bytes);
    }

}
