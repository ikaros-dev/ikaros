package run.ikaros.server.result;

import java.util.List;

/**
 * @author guohao
 * @date 2022/10/04
 */
public class PagingWrap<T> {
    private List<T> content;
    private Integer total = 0;
    private Boolean hasNext = false;
    private Boolean hasPrevious = false;
    private Integer currentIndex = 0;

    public List<T> getContent() {
        return content;
    }

    public PagingWrap<T> setContent(List<T> content) {
        this.content = content;
        return this;
    }

    public Integer getTotal() {
        return total;
    }

    public PagingWrap<T> setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public PagingWrap<T> setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
        return this;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public PagingWrap<T> setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
        return this;
    }

    public Integer getCurrentIndex() {
        return currentIndex;
    }

    public PagingWrap<T> setCurrentIndex(Integer currentIndex) {
        this.currentIndex = currentIndex;
        return this;
    }
}
