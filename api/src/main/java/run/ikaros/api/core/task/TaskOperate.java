package run.ikaros.api.core.task;

import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;

public interface TaskOperate extends AllowPluginOperate {
    Mono<Void> submit(String name, Runnable runnable);

    Mono<Void> cancel(String name);
}
