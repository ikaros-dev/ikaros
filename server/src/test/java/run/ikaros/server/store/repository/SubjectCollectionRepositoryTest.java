package run.ikaros.server.store.repository;


import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class SubjectCollectionRepositoryTest {

    @Autowired
    SubjectCollectionRepository repository;


    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }




}