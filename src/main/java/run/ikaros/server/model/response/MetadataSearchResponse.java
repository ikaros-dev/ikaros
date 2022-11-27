package run.ikaros.server.model.response;

public class MetadataSearchResponse {
    private Integer bgmTvSubjectId;
    private String name;
    private String nameCn;
    private String description;
    private String url;
    private String image;

    public Integer getBgmTvSubjectId() {
        return bgmTvSubjectId;
    }

    public MetadataSearchResponse setBgmTvSubjectId(Integer bgmTvSubjectId) {
        this.bgmTvSubjectId = bgmTvSubjectId;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public MetadataSearchResponse setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public MetadataSearchResponse setName(String name) {
        this.name = name;
        return this;
    }

    public String getNameCn() {
        return nameCn;
    }

    public MetadataSearchResponse setNameCn(String nameCn) {
        this.nameCn = nameCn;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public MetadataSearchResponse setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getImage() {
        return image;
    }

    public MetadataSearchResponse setImage(String image) {
        this.image = image;
        return this;
    }
}
