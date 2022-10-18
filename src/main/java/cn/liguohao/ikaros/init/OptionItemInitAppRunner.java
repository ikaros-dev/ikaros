package cn.liguohao.ikaros.init;

import cn.liguohao.ikaros.service.OptionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Component
public class OptionItemInitAppRunner implements ApplicationRunner {

    private final OptionService optionService;

    public OptionItemInitAppRunner(OptionService optionService) {
        this.optionService = optionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        optionService.initPresetOptionItems();
    }
}
