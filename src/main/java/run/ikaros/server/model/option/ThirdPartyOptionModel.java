package run.ikaros.server.model.option;

import run.ikaros.server.constants.OptionConst.Category;
import run.ikaros.server.constants.OptionConst.Init.ThirdParty;

/**
 * @author guohao
 * @date 2022/10/20
 */
public class ThirdPartyOptionModel implements OptionModel {
    private String category = Category.ThirdParty;
    private String bangumiApiBase = ThirdParty.BANGUMI_API_BASE[1];
    private String bangumiApiSubjects = ThirdParty.BANGUMI_API_SUBJECTS[1];
    private String bangumiApiEpisodes = ThirdParty.BANGUMI_API_EPISODES[1];

    @Override
    public String getCategory() {
        return category;
    }

    public ThirdPartyOptionModel setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getBangumiApiBase() {
        return bangumiApiBase;
    }

    public ThirdPartyOptionModel setBangumiApiBase(String bangumiApiBase) {
        this.bangumiApiBase = bangumiApiBase;
        return this;
    }

    public String getBangumiApiSubjects() {
        return bangumiApiSubjects;
    }

    public ThirdPartyOptionModel setBangumiApiSubjects(String bangumiApiSubjects) {
        this.bangumiApiSubjects = bangumiApiSubjects;
        return this;
    }

    public String getBangumiApiEpisodes() {
        return bangumiApiEpisodes;
    }

    public ThirdPartyOptionModel setBangumiApiEpisodes(String bangumiApiEpisodes) {
        this.bangumiApiEpisodes = bangumiApiEpisodes;
        return this;
    }
}
