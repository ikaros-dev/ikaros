package run.ikaros.server.model.option;

import run.ikaros.server.constants.OptionConst.Category;
import run.ikaros.server.constants.OptionConst.Init.Seo;

/**
 * @author guohao
 * @date 2022/10/19
 */
public class SeoOptionModel implements OptionModel {
    private String category = Category.SEO;
    private String hideForSearchEngine = Seo.HIDE_FOR_SE[1];
    private String keywords = Seo.KEYWORDS[1];
    private String siteDescription = Seo.SITE_DESCRIPTION[1];

    @Override
    public String getCategory() {
        return category;
    }

    public SeoOptionModel setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getHideForSearchEngine() {
        return hideForSearchEngine;
    }

    public SeoOptionModel setHideForSearchEngine(String hideForSearchEngine) {
        this.hideForSearchEngine = hideForSearchEngine;
        return this;
    }

    public String getKeywords() {
        return keywords;
    }

    public SeoOptionModel setKeywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    public String getSiteDescription() {
        return siteDescription;
    }

    public SeoOptionModel setSiteDescription(String siteDescription) {
        this.siteDescription = siteDescription;
        return this;
    }
}
