package run.ikaros.server.store.repository;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.UserRoleEntity;

@Testcontainers
@SpringBootTest
@Import(IkarosTestcontainersConfiguration.class)
class UserRoleRepositoryTest {

    @Autowired
    UserRoleRepository userRoleRepository;

    @Test
    void findByUserIdAndRoleId() {
        // create record
        final UUID id = UuidV7Utils.generateUuid();
        final UUID userId = UuidV7Utils.generateUuid();
        final UUID roleId = UuidV7Utils.generateUuid();
        StepVerifier.create(userRoleRepository.insert(
                    UserRoleEntity.builder()
                        .id(id)
                        .userId(userId).roleId(roleId)
                        .build())
                .map(UserRoleEntity::getRoleId))
            .expectNext(roleId)
            .verifyComplete();

        // find by id
        StepVerifier.create(userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .map(UserRoleEntity::getRoleId))
            .expectNext(roleId)
            .verifyComplete();
    }
}