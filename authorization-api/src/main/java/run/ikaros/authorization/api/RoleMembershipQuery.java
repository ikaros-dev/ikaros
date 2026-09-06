package run.ikaros.authorization.api;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Authorization 向用户目录公开的角色成员查询能力。 */
public interface RoleMembershipQuery {
    Mono<List<String>> roleCodesFor(UUID subjectId);
}
