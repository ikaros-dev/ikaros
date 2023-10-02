package run.ikaros.server.theme;

import reactor.core.publisher.Mono;

public interface ThemeService {
    Mono<String> getCurrentTheme();
}
