package cn.liguohao.ikaros.model.bgmtv;

import java.util.List;

/**
 * @author guohao
 * @date 2022/10/20
 */
public class BgmTvPagingData<T> {
    private List<T> data;
    private Integer total;
    private Integer limit;
    private Integer offset;

    public List<T> getData() {
        return data;
    }

    public BgmTvPagingData<T> setData(List<T> data) {
        this.data = data;
        return this;
    }

    public Integer getTotal() {
        return total;
    }

    public BgmTvPagingData<T> setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public BgmTvPagingData<T> setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public BgmTvPagingData<T> setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }
}
