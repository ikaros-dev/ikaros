package run.ikaros.api.core.episode;

import reactor.core.publisher.Mono;

/**
 * Handler interface for the Episode Sequence Regular Chain of Responsibility.
 * Each handler checks if an attachment name matches its regex pattern.
 * <p>
 * Implementations can be provided by plugins via {@link EpisodeSequenceRegularPluginHook}.
 *
 * @see EpisodeSequenceRegularPluginHook
 */
public interface EpisodeSequenceRegularHandler {

    /**
     * Priority of this handler. Higher values run first.
     */
    int order();

    /**
     * Try to match the given attachment name against this handler's rule.
     *
     * @param attachmentName the attachment name to match
     * @return Mono containing the match result, or {@link Mono#empty()} if no match
     */
    Mono<EpisodeSequenceRegularResult> match(String attachmentName);
}
