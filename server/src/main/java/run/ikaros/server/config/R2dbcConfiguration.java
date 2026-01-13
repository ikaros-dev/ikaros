package run.ikaros.server.config;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import run.ikaros.api.infra.utils.PathResourceUtils;
import run.ikaros.server.store.convert.Enum2StringConverter;
import run.ikaros.server.store.convert.String2EnumConverter;
import run.ikaros.server.store.repository.DelegateBaseRepository;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableR2dbcAuditing
@EnableR2dbcRepositories(
    repositoryBaseClass = DelegateBaseRepository.class,
    basePackages = "run.ikaros.server.store.repository"
)
public class R2dbcConfiguration {

    /**
     * 自定义enum的转化逻辑.
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();

        final String packageName = "run.ikaros.api.store.enums";
        List<Class<?>> classes = new ArrayList<>();
        try {
            classes.addAll(PathResourceUtils.getClasses(packageName));
        } catch (Exception e) {
            log.warn("Get class fail for packageName={}", packageName, e);
        }
        for (Class<?> cls : classes) {
            if (!cls.isEnum()) {
                continue;
            }
            Class<? extends Enum> enumCls = cls.asSubclass(Enum.class);
            converters.add(new String2EnumConverter<>(enumCls));
            converters.add(new Enum2StringConverter<>(enumCls));
        }


        return R2dbcCustomConversions.of(
            PostgresDialect.INSTANCE,
            converters
        );
    }
}
