package run.ikaros.server.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedRuntimeException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionNetwork;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.utils.RestTemplateUtils;

/**
 * @author li-guohao
 */
@RestController
@RequestMapping("/network")
public class NetworkRestController {

    private final BgmTvService bgmTvService;
    private final OptionService optionService;

    public NetworkRestController(BgmTvService bgmTvService, OptionService optionService) {
        this.bgmTvService = bgmTvService;
        this.optionService = optionService;
    }

    @PutMapping("/metadata/bgmTv/subject")
    public CommonResult<AnimeDTO> reqBgmtvBangumiMetadata(@RequestParam("id") Long subjectId)
        throws RecordNotFoundException {
        return CommonResult.ok(bgmTvService.reqBgmtvSubject(subjectId));
    }

    @GetMapping("/proxy/connect/test")
    public CommonResult<Boolean> proxyConnectTest() {
        String httpProxyHost = optionService.getOptionNetworkHttpProxyHost();
        String httpProxyPort = optionService.getOptionNetworkHttpProxyPort();
        return CommonResult.ok(RestTemplateUtils.testProxyConnect(httpProxyHost, httpProxyPort));
    }
}
