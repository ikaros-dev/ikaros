package run.ikaros.server.plugin;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class PluginStartingError implements Serializable {
    private String pluginId;

    private String message;

    private String devMessage;
}
