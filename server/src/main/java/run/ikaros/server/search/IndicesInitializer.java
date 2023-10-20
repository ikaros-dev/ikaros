package run.ikaros.server.search;

import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import run.ikaros.server.custom.scheme.SchemeInitializedEvent;

@Slf4j
@Component
public class IndicesInitializer {

    private final IndicesService indicesService;

    public IndicesInitializer(IndicesService indicesService) {
        this.indicesService = indicesService;
    }

    @Async
    @EventListener(SchemeInitializedEvent.class)
    public void whenSchemeInitialized(SchemeInitializedEvent event) throws InterruptedException {
        initSubjectIndices();
    }

    private void initSubjectIndices() throws InterruptedException {
        var latch = new CountDownLatch(1);
        log.info("Initialize subject indices...");
        var watch = new StopWatch("SubjectIndicesWatch");
        watch.start("subject-indices-rebuild");
        indicesService.rebuildSubjectIndices()
            .doFinally(signalType -> latch.countDown())
            .subscribe();
        latch.await();
        watch.stop();
        log.info("Initialized subject indices. Usage: {}", watch);
    }

}
