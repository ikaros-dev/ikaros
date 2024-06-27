package run.ikaros.server.core.authority;

import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;

@Slf4j
@Component
public class AuthorityInitializer {
    private final AuthorityService authorityService;

    public AuthorityInitializer(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    /**
     * init security authorities.
     *
     * @see run.ikaros.api.constant.SecurityConst
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        return initAuthorities();
    }

    private Mono<Void> initAuthorities() {
        Class<SecurityConst.Authorization.Target> targetClass =
            SecurityConst.Authorization.Target.class;
        return Flux.fromArray(targetClass.getFields())
            .flatMap(this::saveAuthoritiesWithTarget)
            .then();
    }

    private Mono<Void> saveAuthoritiesWithTarget(Field tarField) {
        String tarFieldName = tarField.getName();
        AuthorityType authorityType = AuthorityType.OTHERS;
        if (tarFieldName.equalsIgnoreCase("ALL")) {
            authorityType = AuthorityType.ALL;
        }

        if (tarFieldName.startsWith("API") && !tarFieldName.startsWith("APIS")) {
            authorityType = AuthorityType.API;
        }
        if (tarFieldName.startsWith("APIS")) {
            authorityType = AuthorityType.APIS;
        }
        if (tarFieldName.startsWith("MENU")) {
            authorityType = AuthorityType.MENU;
        }
        if (tarFieldName.startsWith("URL")) {
            authorityType = AuthorityType.URL;
        }

        final String tarFieldValue = String.valueOf(ReflectionUtils.getField(tarField, null));

        final AuthorityType type = authorityType;
        Class<SecurityConst.Authorization.Authority> authorityClass =
            SecurityConst.Authorization.Authority.class;
        return Flux.fromArray(authorityClass.getFields())
            .map(authField -> AuthorityEntity.builder()
                .allow(true)
                .type(type)
                .authority(String.valueOf(ReflectionUtils.getField(authField, null)))
                .target(tarFieldValue)
                .build())
            .flatMap(authorityService::saveEntity)
            .filter(entity -> tarFieldValue.endsWith("/**"))
            .filter(entity ->
                StringUtils.isNotBlank(tarFieldValue.substring(0, tarFieldValue.indexOf("/**"))))
            .map(entity -> AuthorityEntity.builder()
                .allow(entity.getAllow())
                .type(entity.getType())
                .authority(entity.getAuthority())
                .target(tarFieldValue.substring(0, tarFieldValue.indexOf("/**")))
                .build())
            .flatMap(authorityService::saveEntity)
            .then()
            ;
    }
}
