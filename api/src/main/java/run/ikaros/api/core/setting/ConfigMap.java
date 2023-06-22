package run.ikaros.api.core.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.custom.Name;

@Data
@Custom(group = "setting.ikaros.run", version = OpenApiConst.CORE_VERSION, kind = "ConfigMap",
    singular = "configmap", plural = "configmaps")
public class ConfigMap {
    /**
     * 如是插件的配置，会与插件名称(name)保持一致.
     */
    @Name
    private String name;
    private Map<String, Object> data;

    /**
     * Put data map item.
     *
     * @param key      item key
     * @param dataItem item value
     * @return this
     */
    public ConfigMap putDataItem(String key, Object dataItem) {
        if (this.data == null) {
            this.data = new LinkedHashMap<>();
        }
        this.data.put(key, dataItem);
        return this;
    }
}
