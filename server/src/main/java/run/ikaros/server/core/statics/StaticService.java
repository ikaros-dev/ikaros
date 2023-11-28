package run.ikaros.server.core.statics;

import reactor.core.publisher.Flux;

public interface StaticService {
    Flux<String> listStaticsFonts();
}
