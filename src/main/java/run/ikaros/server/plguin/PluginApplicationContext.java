package run.ikaros.server.plguin;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import run.ikaros.server.entity.CustomEntity;

/**
 * @author: li-guohao
 */
public class PluginApplicationContext extends GenericApplicationContext {
    private final MultiValueMap<CustomEntity, String> customEntitiesMapping =
        new LinkedMultiValueMap<>();
}
