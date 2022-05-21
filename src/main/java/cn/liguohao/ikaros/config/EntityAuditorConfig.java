package cn.liguohao.ikaros.config;

import cn.liguohao.ikaros.config.security.UserDetailsAdapter;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 数据库实体编辑的创建人和更新人，配置注解[CreatedBy]和注解[LastModifiedBy]让字段字段更新。
 *
 * @author li-guohao
 */
@Configuration
@EnableJpaAuditing
public class EntityAuditorConfig implements AuditorAware<Long> {

    /**
     * 当没有用户信息的时候，表记录的各更新的UUID就设置为0
     */
    public static final Long UUID_WHEN_NO_AUTH = 0L;

    private static SecurityContext securityContext = SecurityContextHolder.getContext();

    @Override
    public Optional<Long> getCurrentAuditor() {
        Long currentUid = UUID_WHEN_NO_AUTH;
        Authentication authentication = securityContext.getAuthentication();
        // 用户已经登陆，获取uid
        if (authentication != null) {
            UserDetailsAdapter userDetailsAdapter
                = (UserDetailsAdapter) authentication.getPrincipal();
            currentUid = userDetailsAdapter.getUser().getId();
        }

        return Optional.of(currentUid);
    }
}
