package run.ikaros.server.infra.utils;

import org.springframework.beans.BeansException;
import reactor.core.publisher.Mono;

public class ReactiveBeanUtils {
    public static <T> Mono<T> copyProperties(Object source, T target) throws BeansException {
        org.springframework.beans.BeanUtils.copyProperties(source, target);
        return Mono.just(target);
    }
}
