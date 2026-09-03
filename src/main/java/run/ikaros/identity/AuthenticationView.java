package run.ikaros.identity; import java.time.Instant; import java.util.UUID; public record AuthenticationView(UUID userId,UUID sessionId,String sessionToken,Instant expiresAt,UserView user) {}
