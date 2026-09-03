package run.ikaros.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 提供角色和平台权限注册表的管理接口。
 */
@RestController
@RequestMapping({"/api/roles", "/api/v2/admin/roles"})
public class RoleController {
    private final RoleService roleService;

    /**
     * 创建角色控制器。
     *
     * @param roleService 平台角色服务
     */
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 创建自定义平台角色。
     *
     * @param actorId 当前管理主体
     * @param request 角色资料
     * @return 新建角色视图
     */
    @Operation(summary = "创建平台角色", description = "创建一个可绑定系统声明权限的自定义角色。"
        + "内置角色与自定义角色的删除限制会在后续管理能力中补充。")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "角色创建成功"),
        @ApiResponse(responseCode = "400", description = "角色资料或管理主体不合法", content = @Content),
        @ApiResponse(responseCode = "409", description = "角色编码已存在", content = @Content)
    })
    @PostMapping
    public Mono<ResponseEntity<RoleView>> create(
        @Parameter(description = "执行角色管理操作的当前主体 UUID", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @Valid @RequestBody CreateRoleRequest request
    ) {
        return roleService.create(actorId, request)
            .map(role -> ResponseEntity.created(URI.create("/api/roles/" + role.id())).body(role));
    }

    /**
     * 列出角色及其已授权权限。
     *
     * @return 角色视图流
     */
    @Operation(summary = "浏览平台角色", description = "返回全部角色及其权限键。权限键只能来自平台核心或插件声明的注册表。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "角色列表查询成功")})
    @GetMapping
    public Flux<RoleView> list() {
        return roleService.list();
    }

    /**
     * 为角色授予一项已声明的平台权限。
     *
     * @param actorId 当前管理主体
     * @param roleId 角色标识
     * @param permission 平台声明的权限枚举
     * @return 更新后的角色视图
     */
    @Operation(summary = "为角色授予权限", description = "将平台预先声明的权限绑定给角色。"
        + "接口不接受任意字符串，避免保存没有实际效果的伪权限。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "权限授予成功或已存在"),
        @ApiResponse(responseCode = "404", description = "角色不存在", content = @Content)
    })
    @PostMapping("/{roleId}/permissions/{permission}")
    public Mono<RoleView> grantPermission(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID roleId,
        @PathVariable PlatformPermission permission
    ) {
        return roleService.grantPermission(actorId, roleId, permission);
    }

    @PutMapping("/{roleId}/permissions")
    public Mono<RoleView> replacePermissions(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID roleId,
        @Valid @RequestBody ReplaceRolePermissionsRequest request
    ) {
        return roleService.replacePermissions(actorId, roleId, request);
    }
}
