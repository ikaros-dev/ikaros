package cn.liguohao.ikaros.model.bgmtv;

import java.util.List;

/**
 * @author guohao
 * @date 2022/10/20
 */
public class PagingData<T> {
    private List<T> data;
    private Integer total;
    private Integer limit;
    private Integer offset;

    public List<T> getData() {
        return data;
    }

    public PagingData<T> setData(List<T> data) {
        this.data = data;
        return this;
    }

    public Integer getTotal() {
        return total;
    }

    public PagingData<T> setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public PagingData<T> setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public PagingData<T> setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }
}
