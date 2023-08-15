package run.ikaros.server.infra.utils;

import java.util.Objects;
import org.springframework.beans.BeansException;
import reactor.core.publisher.Mono;

public class ReactiveBeanUtils {
    /**
     * Copy properties by reactive.
     */
    public static <T> Mono<T> copyProperties(Object source, T target) throws BeansException {
        if (Objects.isNull(source)) {
            return Mono.empty();
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target);
        return Mono.just(target);
    }

    /**
     * Copy properties by reactive.
     */
    public static <T> Mono<T> copyProperties(Object source, T target, String... ignoreProperties)
        throws BeansException {
        if (Objects.isNull(source)) {
            return Mono.empty();
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
        return Mono.just(target);
    }
}
