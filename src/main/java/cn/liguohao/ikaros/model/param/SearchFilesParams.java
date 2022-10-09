package cn.liguohao.ikaros.model.param;

import cn.liguohao.ikaros.model.file.IkarosFile;

/**
 * @author guohao
 * @date 2022/10/10
 */
public class SearchFilesParams {
    private Integer page;
    private Integer size;
    private String keyword;
    /**
     * @see  IkarosFile#getTypeStr()
     */
    private String type;
    private String place;

    public Integer getPage() {
        return page;
    }

    public SearchFilesParams setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public SearchFilesParams setSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getKeyword() {
        return keyword;
    }

    public SearchFilesParams setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    public String getType() {
        return type;
    }

    public SearchFilesParams setType(String type) {
        this.type = type;
        return this;
    }

    public String getPlace() {
        return place;
    }

    public SearchFilesParams setPlace(String place) {
        this.place = place;
        return this;
    }
}
