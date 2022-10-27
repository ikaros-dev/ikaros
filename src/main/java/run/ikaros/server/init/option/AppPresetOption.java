package run.ikaros.server.init.option;

import javax.annotation.Nonnull;
import run.ikaros.server.enums.OptionCategory;

/**
 * @author li-guohao
 */
public class AppPresetOption implements PresetOption {

    private String isInit = "true";

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
}
