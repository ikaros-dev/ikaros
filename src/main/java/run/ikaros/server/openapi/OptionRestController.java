package run.ikaros.server.openapi;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.init.option.AppPresetOption;
import run.ikaros.server.init.option.CommonPresetOption;
import run.ikaros.server.init.option.FilePresetOption;
import run.ikaros.server.init.option.OtherPresetOption;
import run.ikaros.server.init.option.PresetOption;
import run.ikaros.server.init.option.SeoPresetOption;
import run.ikaros.server.init.option.ThirdPartyPresetOption;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.core.service.OptionService;

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

    @GetMapping("/preset/list")
    public CommonResult<List<PresetOption>> findPresetOptionList() {
        return CommonResult.ok(optionService.findPresetOptionList());
    }

    @PutMapping("/preset/app")
    public CommonResult<PresetOption> saveAppPresetOption(
        @RequestBody AppPresetOption presetOption) {
        return CommonResult.ok(optionService.savePresetOption(presetOption));
    }

    @PutMapping("/preset/common")
    public CommonResult<PresetOption> saveCommonPresetOption(
        @RequestBody CommonPresetOption presetOption) {
        return CommonResult.ok(optionService.savePresetOption(presetOption));
    }

    @PutMapping("/preset/file")
    public CommonResult<PresetOption> saveFilePresetOption(
        @RequestBody FilePresetOption presetOption) {
        return CommonResult.ok(optionService.savePresetOption(presetOption));
    }

    @PutMapping("/preset/other")
    public CommonResult<PresetOption> saveOtherPresetOption(
        @RequestBody OtherPresetOption presetOption) {
        return CommonResult.ok(optionService.savePresetOption(presetOption));
    }

    @PutMapping("/preset/seo")
    public CommonResult<PresetOption> saveSeoPresetOption(
        @RequestBody SeoPresetOption presetOption) {
        return CommonResult.ok(optionService.savePresetOption(presetOption));
    }

    @PutMapping("/preset/thirdparty")
    public CommonResult<PresetOption> saveThirdPartyPresetOption(
        @RequestBody ThirdPartyPresetOption presetOption) {
        return CommonResult.ok(optionService.savePresetOption(presetOption));
    }




}
