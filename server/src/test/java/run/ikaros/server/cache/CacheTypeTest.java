package run.ikaros.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CacheTypeTest {

    @Test
    void memoryValueExists() {
        assertThat(CacheType.Memory).isNotNull();
        assertThat(CacheType.Memory.name()).isEqualTo("Memory");
    }

    @Test
    void redisValueExists() {
        assertThat(CacheType.Redis).isNotNull();
        assertThat(CacheType.Redis.name()).isEqualTo("Redis");
    }

    @Test
    void valuesLengthEqualsTwo() {
        assertThat(CacheType.values()).hasSize(2);
    }
}
