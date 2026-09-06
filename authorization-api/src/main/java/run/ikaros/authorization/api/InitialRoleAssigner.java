package run.ikaros.authorization.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** 用户创建流程使用的初始角色分配能力，不暴露角色绑定持久化模型。 */
public interface InitialRoleAssigner {
    Mono<Void> assignInitialRole(UUID subjectId, String roleCode);
}
