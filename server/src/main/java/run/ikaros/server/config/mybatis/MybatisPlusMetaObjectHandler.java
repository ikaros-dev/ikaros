package run.ikaros.server.config.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.SecurityUser;

@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 严格填充模式，如果实体类中该字段有值则不会覆盖
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 获取当前登录用户ID
        UUID currentUserId = getCurrentUserId();
        // 填充创建人
        this.strictInsertFill(metaObject, "createUid", UUID.class, currentUserId);
        // 填充更新人
        this.strictInsertFill(metaObject, "updateUid", UUID.class, currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 严格填充模式，仅当字段值为 null 或未设置时才填充
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 填充更新人
        this.strictUpdateFill(metaObject, "updateUid", UUID.class, getCurrentUserId());
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 未认证或匿名用户
        if (authentication == null
            || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser user) {
            return user.getId();
        }

        return null;
    }
}
