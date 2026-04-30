package run.ikaros.server.cache.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

class CacheRedisEnableConditionTest {

    @Test
    void shouldMatchWhenEnableTrueAndTypeRedis() {
        CacheRedisEnableCondition condition = new CacheRedisEnableCondition();
        ConditionContext context = mock(ConditionContext.class);
        Environment env = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(env);
        when(env.getProperty("ikaros.cache.enable")).thenReturn("true");
        when(env.getProperty("ikaros.cache.type")).thenReturn("redis");

        var outcome = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class));
        assertThat(outcome.isMatch()).isTrue();
    }

    @Test
    void shouldNotMatchWhenEnableFalse() {
        CacheRedisEnableCondition condition = new CacheRedisEnableCondition();
        ConditionContext context = mock(ConditionContext.class);
        Environment env = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(env);
        when(env.getProperty("ikaros.cache.enable")).thenReturn("false");
        when(env.getProperty("ikaros.cache.type")).thenReturn("redis");

        var outcome = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class));
        assertThat(outcome.isMatch()).isFalse();
    }

    @Test
    void shouldNotMatchWhenTypeMemory() {
        CacheRedisEnableCondition condition = new CacheRedisEnableCondition();
        ConditionContext context = mock(ConditionContext.class);
        Environment env = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(env);
        when(env.getProperty("ikaros.cache.enable")).thenReturn("true");
        when(env.getProperty("ikaros.cache.type")).thenReturn("memory");

        var outcome = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class));
        assertThat(outcome.isMatch()).isFalse();
    }

    @Test
    void shouldNotMatchWhenEnableNull() {
        CacheRedisEnableCondition condition = new CacheRedisEnableCondition();
        ConditionContext context = mock(ConditionContext.class);
        Environment env = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(env);
        when(env.getProperty("ikaros.cache.enable")).thenReturn(null);
        when(env.getProperty("ikaros.cache.type")).thenReturn("redis");

        var outcome = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class));
        assertThat(outcome.isMatch()).isFalse();
    }

    @Test
    void shouldNotMatchWhenTypeNull() {
        CacheRedisEnableCondition condition = new CacheRedisEnableCondition();
        ConditionContext context = mock(ConditionContext.class);
        Environment env = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(env);
        when(env.getProperty("ikaros.cache.enable")).thenReturn("true");
        when(env.getProperty("ikaros.cache.type")).thenReturn(null);

        var outcome = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class));
        assertThat(outcome.isMatch()).isFalse();
    }
}
