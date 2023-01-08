package run.ikaros.server.custom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.infra.exception.NotFoundException;

@SpringBootTest
class ReactiveCustomClientTest {

    @Autowired
    ReactiveCustomClient reactiveCustomClient;

    @AfterEach
    void tearDown() {
        reactiveCustomClient.deleteAll().block();
    }

    @Test
    void createAndFetch() {
        String title = "title 001";
        DemoCustom demoCustom = new DemoCustom();
        demoCustom.setHead(Byte.parseByte("1"))
            .setFlag(Boolean.TRUE)
            .setTitle(title)
            .setUser(new DemoCustom.User().setUsername("user"));

        StepVerifier.create(reactiveCustomClient.fetch(DemoCustom.class, title))
            .expectError(NotFoundException.class)
            .verify();

        StepVerifier.create(reactiveCustomClient.create(demoCustom)
            .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
            .expectNext(demoCustom.getTitle())
            .verifyComplete();

        StepVerifier.create(reactiveCustomClient.fetch(DemoCustom.class, title)
            .flatMap(demoCustom1 -> Mono.just(demoCustom1.getUser()))
            .flatMap(user -> Mono.just(user.getUsername()))
        ).expectNext("user")
            .verifyComplete();
    }

    @Test
    void createWithOnlyNameField() {
        DemoOnlyNameCustom demoOnlyNameCustom = new DemoOnlyNameCustom().setTitle("title");
        StepVerifier.create(reactiveCustomClient.create(demoOnlyNameCustom)
            .flatMap(demoOnlyNameCustom1 -> Mono.just(demoOnlyNameCustom1.getTitle())))
            .expectNext("title")
            .verifyComplete();
    }

    @Test
    void update() {
    }

    @Test
    void delete() {
    }

    @Test
    void fetch() {
    }

    @Test
    void list() {
    }

    @Test
    void testList() {
    }
}