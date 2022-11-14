package run.ikaros.server.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import run.ikaros.server.init.option.AppPresetOption;
import run.ikaros.server.core.service.OptionService;

import javax.annotation.Resource;

/**
 * @author li-guohao
 */
@SpringBootTest
class OptionServiceImplTest {

    @Resource
    OptionService optionService;

    @Test
    void initPresetOptionsOnce() {
        optionService.initPresetOptionsOnce();
        optionService.initPresetOptionsOnce();
        optionService.initPresetOptionsOnce();
        AppPresetOption appPresetOption = optionService.findPresetOption(new AppPresetOption());
        Assertions.assertNotNull(appPresetOption);
        Assertions.assertEquals("true", appPresetOption.getIsInit());
    }
}