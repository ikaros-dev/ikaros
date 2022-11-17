package run.ikaros.server.init;

import org.springframework.stereotype.Component;
import run.ikaros.server.core.service.OptionService;
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
