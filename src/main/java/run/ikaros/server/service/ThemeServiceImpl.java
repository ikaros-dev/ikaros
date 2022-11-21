package run.ikaros.server.service;

import java.io.File;
import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.constants.DefaultConst;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.ThemeService;
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
        String theme = DefaultConst.OPTION_APP_THEME;
        return AppConst.PAGE_POSTFIX + File.separator
            + theme + File.separator;
    }

}
