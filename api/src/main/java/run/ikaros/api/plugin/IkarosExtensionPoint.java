package run.ikaros.api.plugin;

import org.pf4j.ExtensionPoint;

/**
 * Base extension point for all Ikaros plugin extension points.
 * All extension point interfaces should extend this instead of {@link ExtensionPoint} directly.
 */
public interface IkarosExtensionPoint extends ExtensionPoint {

    /**
     * Extension point name, used for display and filtering.
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Extension point description.
     */
    default String description() {
        return "";
    }
}
