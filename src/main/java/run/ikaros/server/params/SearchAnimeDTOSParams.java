package run.ikaros.server.params;

/**
 * @author guohao
 * @date 2022/10/22
 */
public class SearchAnimeDTOSParams {
    private Integer page;
    private Integer size;
    private String title;
    private String originalTitle;

    public Integer getPage() {
        return page;
    }

    public SearchAnimeDTOSParams setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public SearchAnimeDTOSParams setSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public SearchAnimeDTOSParams setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public SearchAnimeDTOSParams setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }
}
