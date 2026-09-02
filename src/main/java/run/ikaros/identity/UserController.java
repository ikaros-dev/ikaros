package run.ikaros.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

/**
 * 提供平台用户目录、状态和角色绑定的管理接口。
 */
@Validated
@RestController
@RequestMapping({"/api/users", "/api/v2/admin/users"})
public class UserController {
    private final UserService userService;

    /**
     * 创建用户控制器。
     *
     * @param userService 平台用户服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建一个待激活的平台用户。
     *
     * @param actorId 当前管理主体
     * @param request 用户资料
     * @return 新建用户视图
     */
    @Operation(summary = "创建平台用户", description = "创建不包含密码或令牌的新平台用户。"
        + "新用户初始状态为 PENDING，后续认证 Provider 与激活流程才能建立其会话。")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "用户创建成功"),
        @ApiResponse(responseCode = "400", description = "用户资料或管理主体不合法", content = @Content),
        @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在", content = @Content)
    })
    @PostMapping
    public Mono<ResponseEntity<UserView>> create(
        @Parameter(description = "执行用户管理操作的当前主体 UUID", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.create(actorId, request)
            .map(user -> ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user));
    }

    /**
     * 分页浏览平台用户目录。
     *
     * @param status 可选用户状态
     * @param query 可选用户名关键词
     * @param page 从零开始的页码
     * @param size 每页数量
     * @return 用户分页视图
     */
    @Operation(summary = "浏览平台用户", description = "按状态与用户名关键词分页查询用户。"
        + "响应中仅包含公开管理资料和角色编码，不会返回任何凭据。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "用户列表查询成功"),
        @ApiResponse(responseCode = "400", description = "分页参数不合法", content = @Content)
    })
    @GetMapping
    public Mono<PageResponse<UserView>> list(
        @RequestParam(required = false) UserStatus status,
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return userService.list(status, query, page, size);
    }

    @GetMapping("/{userId}")
    public Mono<UserView> get(@PathVariable UUID userId) {
        return userService.get(userId);
    }

    /**
     * 修改指定用户的生命周期状态。
     *
     * @param actorId 当前管理主体
     * @param userId 用户标识
     * @param status 目标用户状态
     * @return 更新后的用户视图
     */
    @Operation(summary = "修改用户状态", description = "将用户切换为 ACTIVE、DISABLED、LOCKED 等状态并写入审计记录。"
        + "认证网关接入后将以此状态阻止被禁用或锁定用户的新受保护请求。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "用户状态更新成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content)
    })
    @PostMapping("/{userId}/status/{status}")
    public Mono<UserView> changeStatus(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID userId,
        @PathVariable UserStatus status
    ) {
        return userService.changeStatus(actorId, userId, status);
    }

    /**
     * 为用户绑定一个平台角色。
     *
     * @param actorId 当前管理主体
     * @param userId 用户标识
     * @param roleId 角色标识
     * @return 无响应体的完成信号
     */
    @Operation(summary = "为用户分配角色", description = "建立用户与角色的幂等绑定，并写入审计记录。"
        + "平台权限通过角色组合获得，高等级身份验证不会自动增加角色权限。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "角色已分配或原本已存在"),
        @ApiResponse(responseCode = "404", description = "用户或角色不存在", content = @Content)
    })
    @PostMapping("/{userId}/roles/{roleId}")
    public Mono<ResponseEntity<Void>> assignRole(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID userId,
        @PathVariable UUID roleId
    ) {
        return userService.assignRole(actorId, userId, roleId).thenReturn(ResponseEntity.noContent().build());
    }
}
