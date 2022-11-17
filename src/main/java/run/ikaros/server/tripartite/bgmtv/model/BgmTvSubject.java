package run.ikaros.server.tripartite.bgmtv.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 条目
 *
 */
public class BgmTvSubject {
    private Integer id;
    private BgmTvSubjectType type;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String summary;
    /**
     * YYYY-MM-DD
     */
    private String date;
    private String platform;
    private String url;
    private BgmTvImages images;
    private List<BgmTvTag> tags;

    public Integer getId() {
        return id;
    }

    public BgmTvSubject setId(Integer id) {
        this.id = id;
        return this;
    }

    public BgmTvSubjectType getType() {
        return type;
    }

    public BgmTvSubject setType(BgmTvSubjectType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public BgmTvSubject setName(String name) {
        this.name = name;
        return this;
    }

    public String getNameCn() {
        return nameCn;
    }

    public BgmTvSubject setNameCn(String nameCn) {
        this.nameCn = nameCn;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public BgmTvSubject setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDate() {
        return date;
    }

    public BgmTvSubject setDate(String date) {
        this.date = date;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public BgmTvSubject setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public BgmTvSubject setUrl(String url) {
        this.url = url;
        return this;
    }

    public BgmTvImages getImages() {
        return images;
    }

    public BgmTvSubject setImages(BgmTvImages images) {
        this.images = images;
        return this;
    }

    public List<BgmTvTag> getTags() {
        return tags;
    }

    public BgmTvSubject setTags(List<BgmTvTag> tags) {
        this.tags = tags;
        return this;
    }
}
