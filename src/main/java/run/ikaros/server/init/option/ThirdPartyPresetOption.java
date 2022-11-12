package run.ikaros.server.init.option;

import run.ikaros.server.enums.OptionCategory;

import javax.annotation.Nonnull;

/**
 * @author li-guohao
 */
public class ThirdPartyPresetOption implements PresetOption {

    private String bangumiApiBase = "https://api.bgm.tv";
    private String bangumiApiSubjects = "/v0/subjects";
    private String bangumiApiEpisodes = "/v0/episodes";
    private String bangumiApiSearchSubject = "/search/subject";
    /**
     * env var name: IKAROS_SUB_MIKAN_RSS
     */
    private String mikanMySubscribeRssUrl = System.getenv("IKAROS_SUB_MIKAN_RSS");
    /**
     * 要求jellyfin的目录和 ikaros在同一个文件系统下, 因为会创建文件硬链接。
     */
    private String jellyfinMediaDirPath = "/media/ikaros";

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

    public String getMikanMySubscribeRssUrl() {
        return mikanMySubscribeRssUrl;
    }

    public ThirdPartyPresetOption setMikanMySubscribeRssUrl(String mikanMySubscribeRssUrl) {
        this.mikanMySubscribeRssUrl = mikanMySubscribeRssUrl;
        return this;
    }

    public String getJellyfinMediaDirPath() {
        return jellyfinMediaDirPath;
    }

    public ThirdPartyPresetOption setJellyfinMediaDirPath(String jellyfinMediaDirPath) {
        this.jellyfinMediaDirPath = jellyfinMediaDirPath;
        return this;
    }
}
