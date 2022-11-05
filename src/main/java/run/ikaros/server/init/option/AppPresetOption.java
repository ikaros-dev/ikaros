package run.ikaros.server.init.option;

import javax.annotation.Nonnull;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.enums.OptionCategory;

/**
 * @author li-guohao
 */
public class AppPresetOption implements PresetOption {

    private String isInit = "true";
    private String theme = AppConst.DEFAULT_THEME;

    @Nonnull
    @Override
    public OptionCategory getCategory() {
        return OptionCategory.APP;
    }

    public String getIsInit() {
        return isInit;
    }

    public AppPresetOption setIsInit(String isInit) {
        this.isInit = isInit;
        return this;
    }

    public String getTheme() {
        return theme;
    }

    public AppPresetOption setTheme(String theme) {
        this.theme = theme;
        return this;
    }
}
