package run.ikaros.api.core.binding;

import reactor.core.publisher.Mono;

/**
 * Handler interface for the directory binding responsibility chain.
 * Each step handles one aspect of the binding process.
 */
public interface DirectoryBindingStep {

    /**
     * Unique step name for status tracking.
     */
    String name();

    /**
     * Execution order priority. Lower runs first.
     * Built-in steps use 10, 20, 30... with gaps for plugins to insert.
     */
    int order();

    /**
     * Whether this step should be skipped given the current context.
     */
    default boolean shouldSkip(DirectoryBindingContext context) {
        return false;
    }

    /**
     * Execute this step. Returns the updated context.
     */
    Mono<DirectoryBindingContext> execute(DirectoryBindingContext context);

    /**
     * Undo changes made by this step. Called in reverse order on chain failure.
     */
    Mono<Void> rollback(DirectoryBindingContext context);
}
