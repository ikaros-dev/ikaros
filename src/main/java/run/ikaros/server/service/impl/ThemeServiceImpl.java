package run.ikaros.server.service.impl;

import java.io.File;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.init.option.AppPresetOption;
import run.ikaros.server.service.OptionService;
import run.ikaros.server.service.ThemeService;
import run.ikaros.server.utils.StringUtils;

/**
 * @author li-guohao
 */
@Service
public class ThemeServiceImpl implements ThemeService {
    private final OptionService optionService;

    public ThemeServiceImpl(OptionService optionService) {
        this.optionService = optionService;
    }

    @Nonnull
    @Override
    public String getComplexPagePostfix() {
        AppPresetOption appPresetOption = optionService.findPresetOption(new AppPresetOption());
        String theme = StringUtils.isBlank(appPresetOption.getTheme())
            ? AppConst.DEFAULT_THEME : appPresetOption.getTheme();
        return AppConst.PAGE_POSTFIX + File.separator
            + theme + File.separator;
    }

}
