package run.ikaros.server.store.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import run.ikaros.server.test.store.repository.base.BaseTestEntity;
import run.ikaros.server.test.store.repository.base.BaseTestRepository;

/**
 * BaseRepository unit test.
 *
 * @author: li-guohao
 * @see BaseRepository
 */
@DataJpaTest
public class BaseRepositoryTests {

    @Autowired
    BaseTestRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void create() {
        BaseTestEntity baseTestEntity = new BaseTestEntity();
        baseTestEntity.setName("test name");
        repository.saveAndFlush(baseTestEntity);

        Long id = baseTestEntity.getId();
        assertThat(id).isNotNull();
        assertThat(repository.existsById(id)).isTrue();
    }

    @Test
    void update() {
        final String oldName = "old name";
        final String newName = "new name";
        BaseTestEntity baseTestEntity = new BaseTestEntity();
        baseTestEntity.setName(oldName);
        repository.saveAndFlush(baseTestEntity);
        Long id = baseTestEntity.getId();
        assertThat(id).isNotNull();

        Optional<BaseTestEntity> existsEntityOptional = repository.findById(id);
        assertThat(existsEntityOptional).isPresent();
        BaseTestEntity existsEntity = existsEntityOptional.get();
        assertThat(existsEntity.getId()).isEqualTo(id);
        assertThat(existsEntity).isNotNull();
        assertThat(existsEntity.getName()).isEqualTo(oldName);

        existsEntity.setName(newName);
        repository.saveAndFlush(existsEntity);
        existsEntityOptional = repository.findById(existsEntity.getId());
        assertThat(existsEntityOptional).isPresent();
        assertThat(existsEntityOptional.get()).isNotNull();
        assertThat(existsEntityOptional.get().getName()).isEqualTo(newName);
    }

    @Test
    void delete() {
        BaseTestEntity baseTestEntity = new BaseTestEntity();
        baseTestEntity.setName("name");
        repository.saveAndFlush(baseTestEntity);
        Long id = baseTestEntity.getId();
        assertThat(id).isNotNull();
        assertThat(repository.existsById(id)).isTrue();

        repository.deleteById(id);
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void find() {
        BaseTestEntity baseTestEntity = new BaseTestEntity();
        baseTestEntity.setName("name");
        repository.saveAndFlush(baseTestEntity);
        Long id = baseTestEntity.getId();
        Optional<BaseTestEntity> existsEntityOptional = repository.findById(id);
        assertThat(existsEntityOptional).isPresent();
        BaseTestEntity existsEntity = existsEntityOptional.get();
        assertThat(existsEntity.getId()).isEqualTo(id);
        assertThat(existsEntity).isNotNull();
    }

    @Test
    void existsByIdAndStatus() {
        Long id = -1L;
        assertThat(repository.existsByIdAndStatus(id, true)).isFalse();
        BaseTestEntity baseTestEntity = new BaseTestEntity();
        baseTestEntity.setName("name");
        repository.saveAndFlush(baseTestEntity);
        id = baseTestEntity.getId();
        assertThat(repository.existsByIdAndStatus(id, true)).isTrue();
        assertThat(repository.existsByIdAndStatus(id, false)).isFalse();

        baseTestEntity.setStatus(false);
        repository.saveAndFlush(baseTestEntity);
        assertThat(repository.existsByIdAndStatus(id, true)).isFalse();
        assertThat(repository.existsByIdAndStatus(id, false)).isTrue();
    }

    @Test
    void findByIdAndStatus() {
        Long id = -1L;
        assertThat(repository.findByIdAndStatus(id, true)).isEmpty();

        BaseTestEntity baseTestEntity = new BaseTestEntity();
        baseTestEntity.setName("name");
        repository.saveAndFlush(baseTestEntity);
        id = baseTestEntity.getId();

        Optional<BaseTestEntity> existsEntityOptional = repository.findByIdAndStatus(id, true);
        assertThat(existsEntityOptional).isPresent();
        BaseTestEntity existsEntity = existsEntityOptional.get();
        assertThat(existsEntity.getId()).isEqualTo(baseTestEntity.getId());
        assertThat(existsEntity.getName()).isEqualTo(baseTestEntity.getName());

        assertThat(repository.findByIdAndStatus(id, false)).isEmpty();
    }
}
