package run.ikaros.server.custom;

import java.time.LocalDateTime;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class ReactiveCustomClientTest {

    @Autowired
    ReactiveCustomClient reactiveCustomClient;

    @AfterEach
    void tearDown() {
        StepVerifier.create(reactiveCustomClient.deleteAll())
            .verifyComplete();
    }

    @Test
    void createAndFindOne() {
        String title = "title 001";
        StepVerifier.create(reactiveCustomClient.create(null))
            .expectErrorMessage("'custom' must not null").verify();

        StepVerifier.create(reactiveCustomClient.findOne(null, title))
            .expectErrorMessage("'type' must not null").verify();
        StepVerifier.create(reactiveCustomClient.findOne(DemoCustom.class, ""))
            .expectErrorMessage("'name' must has text").verify();

        DemoCustom demoCustom = new DemoCustom();
        demoCustom.setHead(Byte.parseByte("1"))
            .setFlag(Boolean.TRUE)
            .setTitle(title)
            .setUser(new DemoCustom.User().setUsername("user"));

        StepVerifier.create(reactiveCustomClient.findOne(DemoCustom.class, title))
            .expectError(NotFoundException.class)
            .verify();

        StepVerifier.create(reactiveCustomClient.create(demoCustom)
                .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
            .expectNext(demoCustom.getTitle())
            .verifyComplete();

        StepVerifier.create(reactiveCustomClient.findOne(DemoCustom.class, title)
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
    void updateWithoutField() {
        String title = "title 001";
        LocalDateTime time = LocalDateTime.now();
        DemoCustom demoCustom = new DemoCustom();
        demoCustom.setHead(Byte.parseByte("1"))
            .setFlag(Boolean.TRUE)
            .setTitle(title)
            .setTime(time)
            .setUser(new DemoCustom.User().setUsername("user"));

        StepVerifier.create(reactiveCustomClient.create(demoCustom)
                .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
            .expectNext(title)
            .expectComplete()
            .verify();

        Mono<DemoCustom> findOneResult = reactiveCustomClient.findOne(DemoCustom.class, title);
        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getTime())))
            .expectNext(time)
            .verifyComplete();
        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getFlag())))
            .expectNext(Boolean.TRUE)
            .verifyComplete();
        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getUser())
                .flatMap(user -> Mono.just(user.getUsername()))))
            .expectNext("user")
            .verifyComplete();

        LocalDateTime newTime = LocalDateTime.now();
        demoCustom.setTime(newTime)
            .setFlag(Boolean.FALSE)
            .setUser(new DemoCustom.User().setUsername("newUser"));

        StepVerifier.create(reactiveCustomClient.update(demoCustom))
            .expectNext(demoCustom).verifyComplete();

        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getTime())))
            .expectNext(newTime)
            .verifyComplete();
        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getFlag())))
            .expectNext(Boolean.FALSE)
            .verifyComplete();
        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getUser())
                .flatMap(user -> Mono.just(user.getUsername()))))
            .expectNext("newUser")
            .verifyComplete();

    }

    @Test
    void updateWithNameField() {
        StepVerifier.create(reactiveCustomClient.update(null))
            .expectErrorMessage("'custom' must not null").verify();

        String title = "title 001";
        LocalDateTime time = LocalDateTime.now();
        DemoCustom demoCustom = new DemoCustom();
        demoCustom.setHead(Byte.parseByte("1"))
            .setFlag(Boolean.TRUE)
            .setTitle(title)
            .setTime(time)
            .setUser(new DemoCustom.User().setUsername("user"));

        StepVerifier.create(reactiveCustomClient.create(demoCustom)
                .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
            .expectNext(title)
            .expectComplete()
            .verify();

        Mono<DemoCustom> findOneResult = reactiveCustomClient.findOne(DemoCustom.class, title);
        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getTime())))
            .expectNext(time)
            .verifyComplete();


    }

    @Test
    void delete() {
        StepVerifier.create(reactiveCustomClient.delete(null))
            .expectErrorMessage("'custom' must not null").verify();

        String title = "title 001";
        LocalDateTime time = LocalDateTime.now();
        DemoCustom demoCustom = new DemoCustom();
        demoCustom.setHead(Byte.parseByte("1"))
            .setFlag(Boolean.TRUE)
            .setTitle(title)
            .setTime(time)
            .setUser(new DemoCustom.User().setUsername("user"));

        StepVerifier.create(reactiveCustomClient.create(demoCustom)
                .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
            .expectNext(title)
            .expectComplete()
            .verify();

        Mono<DemoCustom> findOneResult = reactiveCustomClient.findOne(DemoCustom.class, title);
        StepVerifier.create(findOneResult.flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
            .expectNext(title)
            .verifyComplete();

        StepVerifier.create(reactiveCustomClient.delete(demoCustom)
                .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
            .expectNext(title)
            .expectComplete()
            .verify();

        StepVerifier.create(findOneResult)
            .expectError(NotFoundException.class)
            .verify();

        demoCustom.setTitle("newTitle");

        StepVerifier.create(reactiveCustomClient.update(demoCustom))
            .expectError(NotFoundException.class)
            .verify();

    }

    @Test
    void findAll() {
        StepVerifier.create(reactiveCustomClient.findAll(null, null))
            .expectErrorMessage("'type' must not null")
            .verify();

        String titlePrefix = "title-";
        for (int i = 0; i < 8; i++) {
            String title = titlePrefix + i;
            DemoCustom demoCustom = new DemoCustom();
            demoCustom.setFlag(Boolean.TRUE)
                .setTitle(title)
                .setNumber((long) i);

            StepVerifier.create(reactiveCustomClient.create(demoCustom)
                    .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
                .expectNext(title)
                .expectComplete()
                .verify();
        }

        // test findAll
        StepVerifier.create(reactiveCustomClient.findAll(DemoCustom.class, null)
                .flatMap(demoCustom -> Mono.just(demoCustom.getTitle())))
            .expectNext(titlePrefix + 0, titlePrefix + 1, titlePrefix + 2, titlePrefix + 3)
            .expectNext(titlePrefix + 4, titlePrefix + 5, titlePrefix + 6, titlePrefix + 7)
            .verifyComplete();


        // test findAll with predicate
        Predicate<DemoCustom> predicate =
            demoCustom -> (titlePrefix + 1).equals(demoCustom.getTitle());
        StepVerifier.create(reactiveCustomClient.findAll(DemoCustom.class, predicate)
                .flatMap(demoCustom -> Mono.just(demoCustom.getTitle())))
            .expectNext(titlePrefix + 1)
            .verifyComplete();
    }


    @Test
    void findAllWithPage() {
        StepVerifier.create(reactiveCustomClient.findAllWithPage(null, null, null, null))
            .expectError(IllegalArgumentException.class).verify();

        long number = 9;
        String titlePrefix = "title-";
        for (int i = 1; i <= number; i++) {
            String title = titlePrefix + i;
            DemoCustom demoCustom = new DemoCustom();
            demoCustom.setFlag(Boolean.TRUE)
                .setTitle(title)
                .setNumber((long) i);
            StepVerifier.create(reactiveCustomClient.create(demoCustom)
                    .flatMap(demoCustom1 -> Mono.just(demoCustom1.getTitle())))
                .expectNext(title).verifyComplete();
        }

        Mono<PagingWrap<DemoCustom>> findFirstPageWithoutPre =
            reactiveCustomClient.findAllWithPage(DemoCustom.class, 1, 4, null);
        StepVerifier.create(findFirstPageWithoutPre
                .flatMap(pageWarp -> Mono.just(pageWarp.getTotal())))
            .expectNext(number).verifyComplete();
        StepVerifier.create(findFirstPageWithoutPre
                .flatMap(pageWarp -> Mono.just(pageWarp.getTotalPages())))
            .expectNext(3L).verifyComplete();
        StepVerifier.create(findFirstPageWithoutPre
                .flatMapMany(pageWarp -> Flux.fromStream(pageWarp.getItems().stream()))
                .flatMap(demoCustom -> Mono.just(demoCustom.getTitle())))
            .expectNext(titlePrefix + 1, titlePrefix + 2, titlePrefix + 3, titlePrefix + 4)
            .verifyComplete();
        StepVerifier.create(reactiveCustomClient.findAllWithPage(DemoCustom.class, 2, 4, null)
                .flatMapMany(pageWarp -> Flux.fromStream(pageWarp.getItems().stream()))
                .flatMap(demoCustom -> Mono.just(demoCustom.getTitle())))
            .expectNext(titlePrefix + 5, titlePrefix + 6, titlePrefix + 7, titlePrefix + 8)
            .verifyComplete();
        StepVerifier.create(reactiveCustomClient.findAllWithPage(DemoCustom.class, 3, 4, null)
                .flatMapMany(pageWarp -> Flux.fromStream(pageWarp.getItems().stream()))
                .flatMap(demoCustom -> Mono.just(demoCustom.getTitle())))
            .expectNext(titlePrefix + 9)
            .verifyComplete();

        StepVerifier.create(reactiveCustomClient.findAllWithPage(DemoCustom.class, 1, 4,
                    demoCustom -> !(titlePrefix + 3).equals(demoCustom.getTitle()))
                .flatMapMany(pageWarp -> Flux.fromStream(pageWarp.getItems().stream()))
                .flatMap(demoCustom -> Mono.just(demoCustom.getTitle())))
            .expectNext(titlePrefix + 1, titlePrefix + 2, titlePrefix + 4)
            .verifyComplete();
        StepVerifier.create(reactiveCustomClient.findAllWithPage(DemoCustom.class, 1, 4,
                    demoCustom -> (titlePrefix + 3).equals(demoCustom.getTitle()))
                .flatMapMany(pageWarp -> Flux.fromStream(pageWarp.getItems().stream()))
                .flatMap(demoCustom -> Mono.just(demoCustom.getTitle())))
            .expectNext(titlePrefix + 3)
            .verifyComplete();

    }
}