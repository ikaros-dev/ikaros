package run.ikaros.server.model.response;

import java.util.Map;

public class OptionResponse {

    private String category;

    /**
     * 这个是在前端界面的Tab页的key
     */
    private Integer tabKey;

    private Map<String /*key*/, String /*value*/> kvMap;

    public String getCategory() {
        return category;
    }

    public OptionResponse setCategory(String category) {
        this.category = category;
        return this;
    }

    public Integer getTabKey() {
        return tabKey;
    }

    public OptionResponse setTabKey(Integer tabKey) {
        this.tabKey = tabKey;
        return this;
    }

    public Map<String, String> getKvMap() {
        return kvMap;
    }

    public OptionResponse setKvMap(Map<String, String> kvMap) {
        this.kvMap = kvMap;
        return this;
    }
}
