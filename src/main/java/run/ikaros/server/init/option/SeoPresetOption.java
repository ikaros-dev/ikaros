package run.ikaros.server.init.option;

import javax.annotation.Nonnull;
import run.ikaros.server.constants.OptionConst;
import run.ikaros.server.enums.OptionCategory;

/**
 * @author li-guohao
 */
public class SeoPresetOption implements PresetOption {

    private String hideForSearchEngine = "false";
    private String keywords = "ikaros;cms;opensource";
    private String siteDescription = "ikaros, an open source acgmn cms application.";


    @Nonnull
    @Override
    public OptionCategory getCategory() {
        return OptionCategory.SEO;
    }

    public String getHideForSearchEngine() {
        return hideForSearchEngine;
    }

    public SeoPresetOption setHideForSearchEngine(String hideForSearchEngine) {
        this.hideForSearchEngine = hideForSearchEngine;
        return this;
    }

    public String getKeywords() {
        return keywords;
    }

    public SeoPresetOption setKeywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    public String getSiteDescription() {
        return siteDescription;
    }

    public SeoPresetOption setSiteDescription(String siteDescription) {
        this.siteDescription = siteDescription;
        return this;
    }
}
