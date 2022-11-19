package run.ikaros.server.model.dto;

public class OptionDTO {
    private String key;
    private String value;
    private String category;

    public String getKey() {
        return key;
    }

    public OptionDTO setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public OptionDTO setValue(String value) {
        this.value = value;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public OptionDTO setCategory(String category) {
        this.category = category;
        return this;
    }
}
