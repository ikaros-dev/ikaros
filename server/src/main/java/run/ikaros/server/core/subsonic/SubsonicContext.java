package run.ikaros.server.core.subsonic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subsonic 请求上下文，包含认证后的用户信息.
 *
 * @author Nekoli
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubsonicContext {
    private Long userId;
    private String username;
    private boolean authenticated;
}
