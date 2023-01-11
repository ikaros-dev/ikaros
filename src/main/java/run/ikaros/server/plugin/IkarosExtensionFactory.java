package run.ikaros.server.plugin;

import org.pf4j.ExtensionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * <p>Basic implementation for {@link ExtensionFactory}.</p>
 * <p>Uses Springs {@link AutowireCapableBeanFactory} to instantiate a given extension class,
 * so can support {@link Autowired}.</p>
 *
 *
 * @author li-guohao
 */
public class IkarosExtensionFactory implements ExtensionFactory {
    @Override
    public <T> T create(Class<T> extensionClass) {
        return null;
    }
}
