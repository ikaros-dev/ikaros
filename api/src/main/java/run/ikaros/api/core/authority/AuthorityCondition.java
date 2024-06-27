package run.ikaros.api.core.authority;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.AuthorityType;

@Data
@Builder
public class AuthorityCondition {
    @Schema(description = "是否放行")
    private Boolean allow;
    @Schema(implementation = AuthorityType.class, description = "权限的类型")
    private AuthorityType type;
    @Schema(description = "权限的目标方，一般是路径")
    private String target;
    @Schema(description = "操作目标的方式，一般是HTTP的方法")
    private String authority;
}
