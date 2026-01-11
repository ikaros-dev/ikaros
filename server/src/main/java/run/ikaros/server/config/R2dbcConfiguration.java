package run.ikaros.server.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.store.convert.AttachmentDriverType2EnumConverter;
import run.ikaros.server.store.convert.AttachmentDriverType2StringConverter;
import run.ikaros.server.store.convert.AttachmentType2EnumConverter;
import run.ikaros.server.store.convert.Enum2StringConverter;
import run.ikaros.server.store.convert.SubjectRelationType2EnumConverter;
import run.ikaros.server.store.repository.DelegateBaseRepository;

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
        converters.add(new AttachmentDriverType2EnumConverter());
        converters.add(new AttachmentDriverType2StringConverter());
        converters.add(new Enum2StringConverter<AttachmentType>());
        converters.add(new AttachmentType2EnumConverter());
        converters.add(new Enum2StringConverter<SubjectRelationType>());
        converters.add(new SubjectRelationType2EnumConverter());

        return R2dbcCustomConversions.of(
            PostgresDialect.INSTANCE,
            converters
        );
    }
}
