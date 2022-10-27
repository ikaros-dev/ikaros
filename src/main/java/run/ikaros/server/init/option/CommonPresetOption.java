package run.ikaros.server.init.option;

import javax.annotation.Nonnull;
import run.ikaros.server.constants.OptionConst;
import run.ikaros.server.enums.OptionCategory;

/**
 * @author li-guohao
 */
public class CommonPresetOption implements PresetOption {

    private String title = "ikaros cms system";
    private String address = "/";
    private String logo = "/logo.jpg";
    private String favicon = "/favicon.ico";
    private String footer = "";

    @Nonnull
    @Override
    public OptionCategory getCategory() {
        return OptionCategory.COMMON;
    }

    public String getTitle() {
        return title;
    }

    public CommonPresetOption setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public CommonPresetOption setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getLogo() {
        return logo;
    }

    public CommonPresetOption setLogo(String logo) {
        this.logo = logo;
        return this;
    }

    public String getFavicon() {
        return favicon;
    }

    public CommonPresetOption setFavicon(String favicon) {
        this.favicon = favicon;
        return this;
    }

    public String getFooter() {
        return footer;
    }

    public CommonPresetOption setFooter(String footer) {
        this.footer = footer;
        return this;
    }
}
