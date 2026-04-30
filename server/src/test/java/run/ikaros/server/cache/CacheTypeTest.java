package run.ikaros.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CacheTypeTest {

    @Test
    void shouldHaveMemoryValue() {
        assertThat(CacheType.Memory).isNotNull();
        assertThat(CacheType.Memory.name()).isEqualTo("Memory");
    }

    @Test
    void shouldHaveRedisValue() {
        assertThat(CacheType.Redis).isNotNull();
        assertThat(CacheType.Redis.name()).isEqualTo("Redis");
    }

    @Test
    void shouldHaveTwoValues() {
        assertThat(CacheType.values()).hasSize(2);
    }

    @Test
    void shouldConvertFromString() {
        assertThat(CacheType.valueOf("Memory")).isEqualTo(CacheType.Memory);
        assertThat(CacheType.valueOf("Redis")).isEqualTo(CacheType.Redis);
    }
}
