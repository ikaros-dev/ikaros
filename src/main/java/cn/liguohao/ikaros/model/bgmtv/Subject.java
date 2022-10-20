package cn.liguohao.ikaros.model.bgmtv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;

/**
 * 条目
 *
 */
public class Subject {
    private Integer id;
    private SubjectType type;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String summary;
    /**
     * YYYY-MM-DD
     */
    private String date;
    private String platform;
    @JsonProperty("url")
    private String url;
    private Images images;
    @JsonIgnore
    private List<HashMap<String, String>> infobox;
    private List<Tag> tags;

    public Integer getId() {
        return id;
    }

    public Subject setId(Integer id) {
        this.id = id;
        return this;
    }

    public SubjectType getType() {
        return type;
    }

    public Subject setType(SubjectType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public Subject setName(String name) {
        this.name = name;
        return this;
    }

    public String getNameCn() {
        return nameCn;
    }

    public Subject setNameCn(String nameCn) {
        this.nameCn = nameCn;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Subject setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDate() {
        return date;
    }

    public Subject setDate(String date) {
        this.date = date;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public Subject setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Subject setUrl(String url) {
        this.url = url;
        return this;
    }

    public Images getImages() {
        return images;
    }

    public Subject setImages(Images images) {
        this.images = images;
        return this;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Subject setTags(List<Tag> tags) {
        this.tags = tags;
        return this;
    }

    public List<HashMap<String, String>> getInfobox() {
        return infobox;
    }

    public Subject setInfobox(
        List<HashMap<String, String>> infobox) {
        this.infobox = infobox;
        return this;
    }
}
