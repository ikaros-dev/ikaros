package run.ikaros.server.init;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.init.option.PresetOption;
import run.ikaros.server.service.OptionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

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
    @SuppressWarnings({"unchecked", "deprecation"})
    public void run(ApplicationArguments args) throws Exception {
        optionService.initPresetOptionsOnce();
    }
}
