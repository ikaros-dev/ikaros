package run.ikaros.authentication.verification;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** 将 SMS OTP 投递到用户绑定手机号的端口。 */
public interface SmsOtpDelivery {
    Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose);
}
