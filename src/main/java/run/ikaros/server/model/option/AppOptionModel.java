package run.ikaros.server.model.option;

import run.ikaros.server.constants.OptionConst.Category;
import run.ikaros.server.constants.OptionConst.Init.App;

/**
 * @author guohao
 * @date 2022/10/19
 */
public class AppOptionModel implements OptionModel {
    private String category = Category.APP;
    private String isInit = App.IS_INIT[1];

    public String getIsInit() {
        return isInit;
    }

    public AppOptionModel setIsInit(String isInit) {
        this.isInit = isInit;
        return this;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public AppOptionModel setCategory(String category) {
        this.category = category;
        return this;
    }
}
