package run.ikaros.server.openapi;

import run.ikaros.server.result.CommonResult;
import run.ikaros.server.model.option.OptionModel;
import run.ikaros.server.model.option.CommonOptionModel;
import run.ikaros.server.model.option.FileOptionModel;
import run.ikaros.server.model.option.OtherOptionModel;
import run.ikaros.server.model.option.SeoOptionModel;
import run.ikaros.server.model.option.ThirdPartyOptionModel;
import run.ikaros.server.service.OptionService;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @GetMapping("/model/list")
    public CommonResult<List<OptionModel>> findOptionModels()
        throws IllegalAccessException, IOException, NoSuchFieldException, InstantiationException {
        return CommonResult.ok(optionService.findAllOptionModel());
    }

    @PostMapping("/model/common")
    public CommonResult<Object> saveCommonOptionModel(@RequestBody CommonOptionModel model)
        throws IllegalAccessException {
        optionService.saveOptionModel(model);
        return CommonResult.ok("success");
    }

    @PostMapping("/model/seo")
    public CommonResult<Object> saveSeoOptionModel(@RequestBody SeoOptionModel model)
        throws IllegalAccessException {
        optionService.saveOptionModel(model);
        return CommonResult.ok("success");
    }

    @PostMapping("/model/file")
    public CommonResult<Object> saveFileOptionModel(@RequestBody FileOptionModel model)
        throws IllegalAccessException {
        optionService.saveOptionModel(model);
        return CommonResult.ok("success");
    }

    @PostMapping("/model/thirdparty")
    public CommonResult<Object> saveFileOptionModel(@RequestBody ThirdPartyOptionModel model)
        throws IllegalAccessException {
        optionService.saveOptionModel(model);
        return CommonResult.ok("success");
    }

    @PostMapping("/model/other")
    public CommonResult<Object> saveOtherOptionModel(@RequestBody OtherOptionModel model)
        throws IllegalAccessException {
        optionService.saveOptionModel(model);
        return CommonResult.ok("success");
    }


}
