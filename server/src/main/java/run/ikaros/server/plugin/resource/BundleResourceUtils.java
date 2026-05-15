package run.ikaros.server.plugin.resource;

import org.pf4j.PluginWrapper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import run.ikaros.api.plugin.PluginConst;
import run.ikaros.server.infra.utils.PathUtils;
import run.ikaros.server.plugin.IkarosPluginManager;

public abstract class BundleResourceUtils {
    private static final String CONSOLE_BUNDLE_LOCATION = "console";
    private static final String JS_BUNDLE = "index.js";
    private static final String CSS_BUNDLE = "style.css";

    /**
     * Gets plugin css bundle resource path relative to the plugin classpath if exists.
     *
     * @return css bundle resource path if exists, otherwise return null.
     */
    @Nullable
    public static String getCssBundlePath(IkarosPluginManager ikarosPluginManager,
                                          String pluginName) {
        Resource jsBundleResource =
            getJsBundleResource(ikarosPluginManager, pluginName, CSS_BUNDLE);
        if (jsBundleResource != null) {
            return consoleResourcePath(pluginName, CSS_BUNDLE);
        }
        return null;
    }

    private static String consoleResourcePath(String pluginName, String name) {
        return PathUtils.combinePath(PluginConst.assertsRoutePrefix(pluginName),
            CONSOLE_BUNDLE_LOCATION, name);
    }

    /**
     * Gets plugin js bundle resource path relative to the plugin classpath if exists.
     *
     * @return js bundle resource path if exists, otherwise return null.
     */
    @Nullable
    public static String getJsBundlePath(IkarosPluginManager ikarosPluginManager,
                                         String pluginName) {
        Resource jsBundleResource = getJsBundleResource(ikarosPluginManager, pluginName, JS_BUNDLE);
        if (jsBundleResource != null) {
            return consoleResourcePath(pluginName, JS_BUNDLE);
        }
        return null;
    }

    /**
     * Gets js bundle resource by plugin name in console location.
     *
     * @return js bundle resource if exists, otherwise null
     */
    @Nullable
    public static Resource getJsBundleResource(IkarosPluginManager pluginManager, String pluginName,
                                               String bundleName) {
        Assert.hasText(pluginName, "The pluginName must not be blank");
        Assert.hasText(bundleName, "Bundle name must not be blank");

        DefaultResourceLoader resourceLoader = getResourceLoader(pluginManager, pluginName);
        if (resourceLoader == null) {
            return null;
        }
        String path = PathUtils.combinePath(CONSOLE_BUNDLE_LOCATION, bundleName);
        Resource resource = resourceLoader.getResource(path);
        return resource.exists() ? resource : null;
    }

    /**
     * Get resource login by plugin id from plugin manager.
     */
    @Nullable
    public static DefaultResourceLoader getResourceLoader(IkarosPluginManager pluginManager,
                                                          String pluginName) {
        Assert.notNull(pluginManager, "Plugin manager must not be null");
        PluginWrapper plugin = pluginManager.getPlugin(pluginName);
        if (plugin == null) {
            return null;
        }
        return new DefaultResourceLoader(plugin.getPluginClassLoader());
    }
}
