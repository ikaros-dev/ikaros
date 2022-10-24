package run.ikaros.server.model.option;

import run.ikaros.server.constants.OptionConst.Category;
import run.ikaros.server.constants.OptionConst.Init.File;

/**
 * @author guohao
 * @date 2022/10/19
 */
public class FileOptionModel implements OptionModel {
    private String category = Category.FILE;
    private String placeSelect = File.PLACE_SELECT[1];

    @Override
    public String getCategory() {
        return category;
    }

    public FileOptionModel setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getPlaceSelect() {
        return placeSelect;
    }

    public FileOptionModel setPlaceSelect(String placeSelect) {
        this.placeSelect = placeSelect;
        return this;
    }
}
