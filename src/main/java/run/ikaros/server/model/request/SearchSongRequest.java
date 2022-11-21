package run.ikaros.server.model.request;

import org.hibernate.validator.constraints.Length;


public class SearchSongRequest {
    @Length(message = "page must > 0")
    private Integer page;
    @Length(min = 1, message = "size must >= 1")
    private Integer size;
    private String keyword;

    public Integer getPage() {
        return page;
    }

    public SearchSongRequest setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public SearchSongRequest setSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getKeyword() {
        return keyword;
    }

    public SearchSongRequest setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }
}
