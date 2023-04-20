package run.ikaros.server.store.repository;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.store.entity.CustomEntity;

@SpringBootTest
class CustomRepositoryTest {

    @Autowired
    CustomRepository customRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(customRepository.deleteAll())
            .verifyComplete();
    }

    @Test
    void findAllByGroupAndVersionAndKind() {
        String namePrefix = "test-name-";
        String group = "test-group";
        String version = "v1alpha1";
        String kind = "TestCustomRepositoryCustom";
        List<CustomEntity> customEntityList = new ArrayList<>();
        long count = 9;
        for (int i = 1; i <= count; i++) {
            customEntityList.add(
                CustomEntity.builder()
                    .group(group)
                    .version(version)
                    .kind(kind)
                    .name(namePrefix + i)
                    .build());
        }
        StepVerifier.create(customRepository.saveAll(customEntityList)
            .collectList()
            .flatMap(customEntities -> Mono.just(customEntities.size())))
                .expectNext((int) count).verifyComplete();



        StepVerifier.create(customRepository.findAll(Example.of(CustomEntity.builder()
                    .group(group).version(version).kind(kind)
                    .build()))
                .collectList()
                .flatMap(customEntities -> Mono.just(customEntities.size())))
            .expectNext((int) count).verifyComplete();
        StepVerifier.create(customRepository.findAllByGroupAndVersionAndKind(group, version, kind,
                    PageRequest.of(0, 4))
                .flatMap(customEntity -> Mono.just(customEntity.getName())))
            .expectNext(namePrefix + 1, namePrefix + 2, namePrefix + 3, namePrefix + 4)
            .verifyComplete();
        StepVerifier.create(customRepository.findAllByGroupAndVersionAndKind(group, version, kind,
                    PageRequest.of(1, 4))
                .flatMap(customEntity -> Mono.just(customEntity.getName())))
            .expectNext(namePrefix + 5, namePrefix + 6, namePrefix + 7, namePrefix + 8)
            .verifyComplete();
        StepVerifier.create(customRepository.findAllByGroupAndVersionAndKind(group, version, kind,
                    PageRequest.of(2, 4))
                .flatMap(customEntity -> Mono.just(customEntity.getName())))
            .expectNext(namePrefix + 9)
            .verifyComplete();
    }


}