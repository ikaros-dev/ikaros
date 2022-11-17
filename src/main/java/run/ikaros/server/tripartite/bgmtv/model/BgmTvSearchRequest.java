package run.ikaros.server.tripartite.bgmtv.model;

/**
 * @author li-guohao
 */
public class BgmTvSearchRequest {
    private String keyword;
    /**
     * <ul>
     *     <li>match meilisearch 的默认排序，按照匹配程度</li>
     *     <li>heat 收藏人数</li>
     *     <li>rank 排名由高到低</li>
     *     <li>score 评分</li>
     * </ul>
     *
     */
    private String sort = "match";
    private BgmTvSearchFilter filter;


    public String getKeyword() {
        return keyword;
    }

    public BgmTvSearchRequest setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    public String getSort() {
        return sort;
    }

    public BgmTvSearchRequest setSort(String sort) {
        this.sort = sort;
        return this;
    }

    public BgmTvSearchFilter getFilter() {
        return filter;
    }

    public BgmTvSearchRequest setFilter(BgmTvSearchFilter filter) {
        this.filter = filter;
        return this;
    }
}
