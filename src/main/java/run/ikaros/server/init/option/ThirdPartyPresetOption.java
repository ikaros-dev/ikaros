package run.ikaros.server.init.option;

import javax.annotation.Nonnull;
import run.ikaros.server.enums.OptionCategory;

/**
 * @author li-guohao
 */
public class ThirdPartyPresetOption implements PresetOption {

    private String bangumiApiBase = "https://api.bgm.tv";
    private String bangumiApiSubjects = "/v0/subjects";
    private String bangumiApiEpisodes = "/v0/episodes";
    private String bangumiApiSearchSubject = "/search/subject";

    @Nonnull
    @Override
    public OptionCategory getCategory() {
        return OptionCategory.THIRD_PARTY;
    }

    public String getBangumiApiBase() {
        return bangumiApiBase;
    }

    public ThirdPartyPresetOption setBangumiApiBase(String bangumiApiBase) {
        this.bangumiApiBase = bangumiApiBase;
        return this;
    }

    public String getBangumiApiSubjects() {
        return bangumiApiSubjects;
    }

    public ThirdPartyPresetOption setBangumiApiSubjects(String bangumiApiSubjects) {
        this.bangumiApiSubjects = bangumiApiSubjects;
        return this;
    }

    public String getBangumiApiEpisodes() {
        return bangumiApiEpisodes;
    }

    public ThirdPartyPresetOption setBangumiApiEpisodes(String bangumiApiEpisodes) {
        this.bangumiApiEpisodes = bangumiApiEpisodes;
        return this;
    }

    public String getBangumiApiSearchSubject() {
        return bangumiApiSearchSubject;
    }

    public ThirdPartyPresetOption setBangumiApiSearchSubject(String bangumiApiSearchSubject) {
        this.bangumiApiSearchSubject = bangumiApiSearchSubject;
        return this;
    }
}
