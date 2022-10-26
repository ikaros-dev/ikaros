package run.ikaros.server.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import run.ikaros.server.constants.OptionConst;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.init.option.PresetOption;
import run.ikaros.server.service.OptionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import run.ikaros.server.utils.ClassUtils;

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
        // read preset package all PresetOption
        List<Class<? extends PresetOption>> classList = optionService.getPresetOptionClassList();

        // build option entity list by all preset option
        List<OptionEntity> optionEntityList = new ArrayList<>();
        for (Class<?> cls : classList) {
            PresetOption presetOption = (PresetOption) cls.newInstance();
            optionEntityList.addAll(PresetOption.buildEntityListByPresetOption(presetOption));
        }

        // save all option entity
        optionEntityList.forEach(optionService::save);
    }
}
