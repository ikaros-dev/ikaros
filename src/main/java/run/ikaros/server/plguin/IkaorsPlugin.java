package run.ikaros.server.plguin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Ikaros Plugin
 *
 * @author: li-guohao
 */
public class IkaorsPlugin extends Plugin {

    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     *
     * @param wrapper plugin wrapper
     */
    public IkaorsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

}
