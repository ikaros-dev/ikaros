package run.ikaros.server.init.option;

import javax.annotation.Nonnull;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.OptionCategory;

/**
 * @author li-guohao
 */
public class FilePresetOption implements PresetOption {

    private String placeSelect = FilePlace.LOCAL.name();

    @Nonnull
    @Override
    public OptionCategory getCategory() {
        return OptionCategory.FILE;
    }

    public String getPlaceSelect() {
        return placeSelect;
    }

    public FilePresetOption setPlaceSelect(String placeSelect) {
        this.placeSelect = placeSelect;
        return this;
    }
}
