package run.ikaros.server.store.repository;


import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.UserEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    UserRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        UserEntity entity = UserEntity.builder()
            .username("test-user-" + UuidV7Utils.generate().substring(0, 8))
            .password("password123")
            .enable(true)
            .nonLocked(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findByUsernameAndEnableAndDeleteStatus() {
        String username = "test-user-" + UuidV7Utils.generate().substring(0, 8);
        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("password123")
            .enable(true)
            .nonLocked(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByUsernameAndEnableAndDeleteStatus(
                username, true, false))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void existsByUsername() {
        String username = "test-user-" + UuidV7Utils.generate().substring(0, 8);

        StepVerifier.create(repository.existsByUsername(username))
            .expectNext(false).verifyComplete();

        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("password123")
            .enable(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByUsername(username))
            .expectNext(true).verifyComplete();
    }

    @Test
    void existsByEmail() {
        String email = "test-" + UuidV7Utils.generate().substring(0, 8) + "@example.com";

        StepVerifier.create(repository.existsByEmail(email))
            .expectNext(false).verifyComplete();

        UserEntity entity = UserEntity.builder()
            .username("test-user-" + UuidV7Utils.generate().substring(0, 8))
            .password("password123")
            .email(email)
            .enable(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByEmail(email))
            .expectNext(true).verifyComplete();
    }

    @Test
    void updateTelephoneByUsername() {
        String username = "test-user-" + UuidV7Utils.generate().substring(0, 8);
        String telephone = "13800138000";

        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("password123")
            .enable(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.updateTelephoneByUsername(username, telephone))
            .verifyComplete();
    }

    @Test
    void updateEmailByUsername() {
        String username = "test-user-" + UuidV7Utils.generate().substring(0, 8);
        String email = "test-" + UuidV7Utils.generate().substring(0, 8) + "@example.com";

        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("password123")
            .enable(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.updateEmailByUsername(username, email))
            .verifyComplete();
    }

    @Test
    void existsByUsernameAndEnableAndDeleteStatus() {
        String username = "test-user-" + UuidV7Utils.generate().substring(0, 8);

        StepVerifier.create(repository.existsByUsernameAndEnableAndDeleteStatus(
                username, true, false))
            .expectNext(false).verifyComplete();

        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("password123")
            .enable(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByUsernameAndEnableAndDeleteStatus(
                username, true, false))
            .expectNext(true).verifyComplete();
    }

    @Test
    void findByUsernameAndDeleteStatus() {
        String username = "test-user-" + UuidV7Utils.generate().substring(0, 8);

        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("password123")
            .enable(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByUsernameAndDeleteStatus(username, false))
            .expectNext(entity).verifyComplete();
    }
}
