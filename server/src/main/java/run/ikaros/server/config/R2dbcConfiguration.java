package run.ikaros.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import run.ikaros.server.store.repository.DelegateBaseRepository;

@Configuration(proxyBeanMethods = false)
@EnableR2dbcAuditing
@EnableR2dbcRepositories(
    repositoryBaseClass = DelegateBaseRepository.class,
    basePackages = "run.ikaros.server.store.repository"
)
public class R2dbcConfiguration {
}
