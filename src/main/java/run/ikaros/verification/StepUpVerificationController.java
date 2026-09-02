package run.ikaros.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供当前会话的 Email OTP Step-up 接口。
 */
@RestController
@RequestMapping({"/api/security/sessions/{sessionId}/step-up", "/api/v2/security/sessions/{sessionId}/step-up"})
public class StepUpVerificationController {
    private final StepUpVerificationService stepUpService;

    /**
     * 创建 Step-up 控制器。
     *
     * @param stepUpService Step-up 协调服务
     */
    public StepUpVerificationController(StepUpVerificationService stepUpService) {
        this.stepUpService = stepUpService;
    }

    /**
     * 为当前会话发起 Email OTP Step-up 挑战。
     *
     * @param userId 当前认证用户标识
     * @param sessionId 当前会话标识
     * @return 挑战摘要
     */
    @Operation(summary = "发起会话增强验证", description = "为当前用户的指定活跃会话创建 LOGIN_STEP_UP 用途的 Email OTP 挑战。"
        + "该挑战不能用于重置密钥或任何其他安全操作。")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Step-up 挑战已创建"),
        @ApiResponse(responseCode = "404", description = "活跃会话不存在", content = @Content),
        @ApiResponse(responseCode = "409", description = "验证码发送频率过高", content = @Content)
    })
    @PostMapping
    public Mono<ResponseEntity<VerificationChallengeView>> issue(
        @Parameter(description = "当前认证用户 UUID", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId,
        @PathVariable UUID sessionId
    ) {
        return stepUpService.issueEmailOtp(userId, sessionId).map(view -> ResponseEntity.accepted().body(view));
    }

    /**
     * 验证当前会话绑定的 OTP 并提升会话 SVL。
     *
     * @param userId 当前认证用户标识
     * @param sessionId 当前会话标识
     * @param challengeId 挑战标识
     * @param request OTP 输入
     * @return 标准验证结果
     */
    @Operation(summary = "完成会话增强验证", description = "只接受当前会话绑定且用途为 LOGIN_STEP_UP 的 OTP 挑战。"
        + "成功后将该会话提升到结果声明的 SVL；权限仍由 RBAC 单独决定。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "会话已提升安全验证等级"),
        @ApiResponse(responseCode = "404", description = "活跃会话不存在", content = @Content),
        @ApiResponse(responseCode = "409", description = "挑战用途或绑定不匹配，或验证码不可用", content = @Content)
    })
    @PostMapping("/{challengeId}/verify")
    public Mono<VerificationResult> verify(
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId,
        @PathVariable UUID sessionId,
        @PathVariable UUID challengeId,
        @Valid @RequestBody VerifyOtpRequest request
    ) {
        return stepUpService.verifyEmailOtp(userId, sessionId, challengeId, request);
    }
}
