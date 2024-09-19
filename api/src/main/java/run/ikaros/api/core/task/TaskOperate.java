package run.ikaros.api.core.task;

import java.time.Duration;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;

public interface TaskOperate extends AllowPluginOperate {
    Mono<Void> submit(String name, Runnable runnable);

    Mono<Void> submit(String name, Runnable runnable, Duration delay);

    Mono<Void> cancel(String name);
}
