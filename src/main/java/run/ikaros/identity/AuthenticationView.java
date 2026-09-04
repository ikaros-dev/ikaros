package run.ikaros.identity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 登录、注册和刷新接口返回的 JWT 凭据。 */
public record AuthenticationView(UUID userId, UUID sessionId, String accessToken,
                                 String refreshToken, Instant expiresAt, UserView user,
                                 List<String> permissions) { }
