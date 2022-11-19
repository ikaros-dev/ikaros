package run.ikaros.server.model.response;

import java.util.Map;

public class OptionResponse {

    private String category;

    private Map<String /*key*/, String /*value*/> kvMap;

    public String getCategory() {
        return category;
    }

    public OptionResponse setCategory(String category) {
        this.category = category;
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
