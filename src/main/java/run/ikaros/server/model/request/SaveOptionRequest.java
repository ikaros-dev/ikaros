package run.ikaros.server.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class SaveOptionRequest {
    @NotBlank
    private String category;
    @Size(min = 1)
    private Map<String, String> kvMap;

    public String getCategory() {
        return category;
    }

    public SaveOptionRequest setCategory(String category) {
        this.category = category;
        return this;
    }

    public Map<String, String> getKvMap() {
        return kvMap;
    }

    public SaveOptionRequest setKvMap(Map<String, String> kvMap) {
        this.kvMap = kvMap;
        return this;
    }
}
