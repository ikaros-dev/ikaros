package run.ikaros.server.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.model.request.AppInitRequest;
import run.ikaros.server.result.CommonResult;

/**
 * @author guohao
 * @date 2022/10/19
 */
@RestController
@RequestMapping("/option")
public class OptionRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionRestController.class);
    private final OptionService optionService;

    public OptionRestController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping("/app/is-init")
    public CommonResult<Boolean> findAppIsInit() {
        return CommonResult.ok(optionService.findAppIsInit());
    }

    @PostMapping("/app/init")
    public CommonResult<Boolean> reqAppInit(@RequestBody AppInitRequest appInitRequest) {
        return CommonResult.ok(optionService.appInit(appInitRequest));
    }


}
