package run.ikaros.security;

import reactor.core.publisher.Mono;

public final class PrincipalContexts {
    private PrincipalContexts() { }

    public static Mono<PrincipalContext> current() {
        return Mono.deferContextual(context -> context.hasKey(PrincipalContext.CONTEXT_KEY)
            ? Mono.just(context.get(PrincipalContext.CONTEXT_KEY)) : Mono.empty());
    }
}
