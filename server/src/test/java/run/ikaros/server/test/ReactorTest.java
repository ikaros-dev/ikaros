package run.ikaros.server.test;

import reactor.core.publisher.Mono;

public class ReactorTest {
    public static void main(String[] args) {
        boolean exists = true;
        Mono.just(exists)
            .filter(e -> !e)
            .flatMap(e -> Mono.just(e)
                .doOnSuccess(a -> System.out.println(1)))
            .doOnSuccess(e -> System.out.println(2))
            .subscribe();
    }
}
