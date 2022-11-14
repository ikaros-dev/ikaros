package run.ikaros.server.config;

import run.ikaros.server.core.service.UserService;

import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 数据库实体编辑的创建人和更新人，配置注解[CreatedBy]和注解[LastModifiedBy]让字段字段更新。
 *
 * @author li-guohao
 */
@Configuration
@EnableJpaAuditing
public class EntityAuditorConfig implements AuditorAware<Long> {


    @Override
    public Optional<Long> getCurrentAuditor() {
        return Optional.of(UserService.getCurrentLoginUserUid());
    }
}
