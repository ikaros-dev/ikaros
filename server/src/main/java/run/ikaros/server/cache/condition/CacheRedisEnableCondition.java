package run.ikaros.server.cache.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class CacheRedisEnableCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context,
                                            AnnotatedTypeMetadata metadata) {
        String cacheEnable = context.getEnvironment().getProperty("ikaros.cache.enable");
        String cacheType = context.getEnvironment().getProperty("ikaros.cache.type");

        boolean match = "true".equals(cacheEnable) && "redis".equals(cacheType);
        return new ConditionOutcome(match, "Cache enable is true and type is redis");
    }
}
