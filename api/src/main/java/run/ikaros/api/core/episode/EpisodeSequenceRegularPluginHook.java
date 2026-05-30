package run.ikaros.api.core.episode;

import java.util.List;
import run.ikaros.api.plugin.IkarosExtensionPoint;

/**
 * Plugin extension point for registering custom {@link EpisodeSequenceRegularHandler}
 * into the episode sequence regular matching chain.
 * <p>
 * Plugins implement this interface to add regex-based episode sequence matchers
 * alongside DB-configured rules. Each handler is sorted by
 * {@link EpisodeSequenceRegularHandler#order()} with higher values running first.
 * <p>
 * Usage example by a plugin:
 * <pre>{@code
 * @Extension
 * public class MyPluginHook implements EpisodeSequenceRegularPluginHook {
 *     public List<EpisodeSequenceRegularHandler> getAdditionalHandlers() {
 *         return List.of(new MyCustomHandler());
 *     }
 * }
 * }</pre>
 *
 * @see EpisodeSequenceRegularHandler
 * @see IkarosExtensionPoint
 */
public interface EpisodeSequenceRegularPluginHook extends IkarosExtensionPoint {

    /**
     * Custom handlers to insert into the episode sequence regular matching chain.
     * The chain merges these with DB-configured rules and sorts by order descending.
     *
     * @return list of custom handlers (empty list if none)
     */
    List<EpisodeSequenceRegularHandler> getAdditionalHandlers();
}
