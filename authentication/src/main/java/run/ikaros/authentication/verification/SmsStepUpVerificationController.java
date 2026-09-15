package run.ikaros.authentication.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

/** 提供当前用户的 SMS OTP Step-up 接口。 */
@RestController
@RequestMapping("/api/security/step-up/sms")
public class SmsStepUpVerificationController {
    private final StepUpVerificationService stepUpService;

    public SmsStepUpVerificationController(StepUpVerificationService stepUpService) {
        this.stepUpService = stepUpService;
    }

    @Operation(summary = "发起短信增强验证", description = "发起 LOGIN_STEP_UP 用途的 SMS OTP，成功验证后达到 SVL-2。"
        + "当前未接入短信网关时，验证码仅按显式配置输出到服务端控制台。")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "SMS Step-up 挑战已创建"),
        @ApiResponse(responseCode = "404", description = "活跃用户不存在", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "409", description = "验证码发送频率过高", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping
    public Mono<ResponseEntity<VerificationChallengeView>> issue(
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId
    ) {
        return stepUpService.issueSmsOtp(userId).map(view -> ResponseEntity.accepted().body(view));
    }

    @Operation(summary = "完成短信增强验证", description = "验证 SMS OTP 并返回 SVL-2 Verification Grant。")
    @PostMapping("/{challengeId}/verify")
    public Mono<VerificationResult> verify(
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId,
        @PathVariable UUID challengeId,
        @Valid @RequestBody VerifyOtpRequest request
    ) {
        return stepUpService.verifySmsOtp(userId, challengeId, request);
    }

    @DeleteMapping("/{challengeId}")
    public Mono<ResponseEntity<Void>> cancel(
        @RequestHeader("X-Ikaros-Actor-Id") UUID userId,
        @PathVariable UUID challengeId
    ) {
        return stepUpService.cancelSmsOtp(userId, challengeId)
            .thenReturn(ResponseEntity.noContent().build());
    }
}
