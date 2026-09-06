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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供当前用户 Email OTP 挑战的发起、验证和取消接口。
 */
@RestController
@RequestMapping({"/api/security/verification-challenges"})
public class VerificationController {
    private final VerificationProvider emailOtpProvider;

    /**
     * 创建验证挑战控制器。
     *
     * @param emailOtpProvider Email OTP 验证 Provider
     */
    public VerificationController(EmailOtpVerificationProvider emailOtpProvider) {
        this.emailOtpProvider = emailOtpProvider;
    }

    /**
     * 发起用途绑定的 Email OTP 挑战。
     *
     * @param userId 当前认证用户标识
     * @param request 验证用途与关联目标
     * @return 挑战摘要
     */
    @Operation(summary = "发起邮件验证码挑战", description = "为当前用户创建用途绑定、短时有效的一次性验证码挑战。"
        + "响应和审计记录均不包含 OTP 明文；验证码只能通过专用认证邮件投递端口发送。")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "验证挑战已创建"),
        @ApiResponse(responseCode = "404", description = "用户不存在或未配置可验证邮箱", content = @Content),
        @ApiResponse(responseCode = "409", description = "发送频率过高", content = @Content)
    })
    @PostMapping
    public Mono<ResponseEntity<VerificationChallengeView>> issue(
        @Parameter(description = "当前认证用户 UUID", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId,
        @Valid @RequestBody IssueVerificationRequest request
    ) {
        return emailOtpProvider.issue(userId, request).map(view -> ResponseEntity.accepted().body(view));
    }

    /**
     * 验证并一次性消费 Email OTP 挑战。
     *
     * @param userId 当前认证用户标识
     * @param challengeId 挑战标识
     * @param request 六位 OTP 输入
     * @return 标准验证结果
     */
    @Operation(summary = "验证邮件验证码", description = "校验当前用户自己的挑战并在成功后立即消费。"
        + "错误次数达到上限会锁定挑战，验证结果仅表示 SVL-1，不会自动授予任何业务权限。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证成功并返回 SVL-1 结果"),
        @ApiResponse(responseCode = "404", description = "验证挑战不存在", content = @Content),
        @ApiResponse(responseCode = "409", description = "挑战已消费、过期、锁定或验证码错误", content = @Content)
    })
    @PostMapping("/{challengeId}/verify")
    public Mono<VerificationResult> verify(
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId,
        @PathVariable UUID challengeId,
        @Valid @RequestBody VerifyOtpRequest request
    ) {
        return emailOtpProvider.verify(userId, challengeId, request);
    }

    /**
     * 取消尚未使用的验证挑战。
     *
     * @param userId 当前认证用户标识
     * @param challengeId 挑战标识
     * @return 无响应体的完成信号
     */
    @Operation(summary = "取消邮件验证码挑战", description = "取消当前用户仍处于 ISSUED 状态的挑战。"
        + "已验证、已锁定或已过期挑战保持其终态，不会被重新激活。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "挑战已取消或已处于终态"),
        @ApiResponse(responseCode = "404", description = "验证挑战不存在", content = @Content)
    })
    @DeleteMapping("/{challengeId}")
    public Mono<ResponseEntity<Void>> cancel(
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId,
        @PathVariable UUID challengeId
    ) {
        return emailOtpProvider.cancel(userId, challengeId).thenReturn(ResponseEntity.noContent().build());
    }
}
