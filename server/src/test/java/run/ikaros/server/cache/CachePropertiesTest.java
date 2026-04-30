package run.ikaros.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CachePropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        CacheProperties properties = new CacheProperties();
        assertThat(properties.getEnable()).isTrue();
        assertThat(properties.getType()).isNull();
        assertThat(properties.getRedis()).isNotNull();
        assertThat(properties.getEnabled()).isFalse();
    }

    @Test
    void shouldSetEnable() {
        CacheProperties properties = new CacheProperties();
        properties.setEnable(false);
        assertThat(properties.getEnable()).isFalse();
    }

    @Test
    void shouldSetType() {
        CacheProperties properties = new CacheProperties();
        properties.setType(CacheType.Redis);
        assertThat(properties.getType()).isEqualTo(CacheType.Redis);
    }

    @Test
    void shouldSetEnabled() {
        CacheProperties properties = new CacheProperties();
        properties.setEnabled(true);
        assertThat(properties.getEnabled()).isTrue();
    }

    @Test
    void shouldHaveDefaultRedisProperties() {
        CacheProperties.Redis redis = new CacheProperties.Redis();
        assertThat(redis.getHost()).isEqualTo("localhost");
        assertThat(redis.getPort()).isEqualTo(6379);
        assertThat(redis.getPassword()).isEmpty();
        assertThat(redis.getTimeout()).isEqualTo(10000);
        assertThat(redis.getExpirationTime()).isEqualTo(3L * 24 * 60 * 60 * 1000);
    }

    @Test
    void shouldSetRedisHost() {
        CacheProperties.Redis redis = new CacheProperties.Redis();
        redis.setHost("192.168.1.1");
        assertThat(redis.getHost()).isEqualTo("192.168.1.1");
    }

    @Test
    void shouldSetRedisPort() {
        CacheProperties.Redis redis = new CacheProperties.Redis();
        redis.setPort(6380);
        assertThat(redis.getPort()).isEqualTo(6380);
    }

    @Test
    void shouldSetRedisPassword() {
        CacheProperties.Redis redis = new CacheProperties.Redis();
        redis.setPassword("secret");
        assertThat(redis.getPassword()).isEqualTo("secret");
    }

    @Test
    void shouldSetRedisTimeout() {
        CacheProperties.Redis redis = new CacheProperties.Redis();
        redis.setTimeout(5000);
        assertThat(redis.getTimeout()).isEqualTo(5000);
    }

    @Test
    void shouldSetRedisExpirationTime() {
        CacheProperties.Redis redis = new CacheProperties.Redis();
        redis.setExpirationTime(1000L);
        assertThat(redis.getExpirationTime()).isEqualTo(1000L);
    }

    @Test
    void shouldSetRedisProperties() {
        CacheProperties properties = new CacheProperties();
        CacheProperties.Redis redis = new CacheProperties.Redis();
        redis.setHost("10.0.0.1");
        redis.setPort(6380);
        properties.setRedis(redis);
        assertThat(properties.getRedis().getHost()).isEqualTo("10.0.0.1");
        assertThat(properties.getRedis().getPort()).isEqualTo(6380);
    }
}
