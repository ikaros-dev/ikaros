package run.ikaros.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachePropertiesTest {

    private CacheProperties cacheProperties;

    @BeforeEach
    void setUp() {
        cacheProperties = new CacheProperties();
    }

    @Test
    void defaultValues() {
        assertThat(cacheProperties.isEnable()).isFalse();
        assertThat(cacheProperties.getType()).isNull();
        assertThat(cacheProperties.getRedis()).isNotNull();
    }

    @Test
    void setterGetterEnable() {
        cacheProperties.setEnable(true);
        assertThat(cacheProperties.isEnable()).isTrue();

        cacheProperties.setEnable(false);
        assertThat(cacheProperties.isEnable()).isFalse();
    }

    @Test
    void setterGetterType() {
        cacheProperties.setType(CacheType.Memory);
        assertThat(cacheProperties.getType()).isEqualTo(CacheType.Memory);

        cacheProperties.setType(CacheType.Redis);
        assertThat(cacheProperties.getType()).isEqualTo(CacheType.Redis);
    }

    @Test
    void nestedRedisDefaultValues() {
        CacheProperties.Redis redis = cacheProperties.getRedis();
        assertThat(redis.getHost()).isEqualTo("localhost");
        assertThat(redis.getPort()).isEqualTo(6379);
        assertThat(redis.getPassword()).isEmpty();
        assertThat(redis.getTimeout()).isEqualTo(10000);
        assertThat(redis.getExpirationTime()).isEqualTo(3L * 24 * 60 * 60 * 1000);
    }

    @Test
    void nestedRedisSettersAndGetters() {
        CacheProperties.Redis redis = new CacheProperties.Redis();

        redis.setHost("192.168.1.100");
        assertThat(redis.getHost()).isEqualTo("192.168.1.100");

        redis.setPort(6380);
        assertThat(redis.getPort()).isEqualTo(6380);

        redis.setPassword("secret");
        assertThat(redis.getPassword()).isEqualTo("secret");

        redis.setTimeout(5000);
        assertThat(redis.getTimeout()).isEqualTo(5000);

        redis.setExpirationTime(86400000L);
        assertThat(redis.getExpirationTime()).isEqualTo(86400000L);
    }
}
