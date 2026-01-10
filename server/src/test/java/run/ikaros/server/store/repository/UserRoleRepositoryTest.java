package run.ikaros.server.store.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;

@Testcontainers
@SpringBootTest
@Import(IkarosTestcontainersConfiguration.class)
class UserRoleRepositoryTest {

    @Autowired
    UserRoleRepository userRoleRepository;

    @Test
    void findByUserIdAndRoleId() {
        StepVerifier.create(userRoleRepository.deleteAll()).verifyComplete();
    }
}