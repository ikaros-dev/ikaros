package run.ikaros.server.parser;

public class AnimeEpisodeInfo {
    private String name;
    private String chineseTitle;
    private String romajiTitle;
    private Long sequence;
    /**
     * JSON字符数组
     */
    private String tags;
    private String bgmTvSubjectId;

    public String getName() {
        return name;
    }

    public AnimeEpisodeInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getChineseTitle() {
        return chineseTitle;
    }

    public AnimeEpisodeInfo setChineseTitle(String chineseTitle) {
        this.chineseTitle = chineseTitle;
        return this;
    }

    public String getRomajiTitle() {
        return romajiTitle;
    }

    public AnimeEpisodeInfo setRomajiTitle(String romajiTitle) {
        this.romajiTitle = romajiTitle;
        return this;
    }

    public Long getSequence() {
        return sequence;
    }

    public AnimeEpisodeInfo setSequence(Long sequence) {
        this.sequence = sequence;
        return this;
    }

    public String getTags() {
        return tags;
    }

    public AnimeEpisodeInfo setTags(String tags) {
        this.tags = tags;
        return this;
    }

    public String getBgmTvSubjectId() {
        return bgmTvSubjectId;
    }

    public AnimeEpisodeInfo setBgmTvSubjectId(String bgmTvSubjectId) {
        this.bgmTvSubjectId = bgmTvSubjectId;
        return this;
    }
}
