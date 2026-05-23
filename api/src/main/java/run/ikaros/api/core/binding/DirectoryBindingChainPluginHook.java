package run.ikaros.api.core.binding;

import java.util.List;
import run.ikaros.api.plugin.IkarosExtensionPoint;

/**
 * Plugin extension point for inserting custom steps into the directory binding chain.
 */
public interface DirectoryBindingChainPluginHook extends IkarosExtensionPoint {

    /**
     * Custom steps to insert into the chain.
     */
    List<DirectoryBindingStep> getAdditionalSteps();
}
