package run.ikaros.server.core.user;

import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserAvatarUpdateEvent extends ApplicationEvent {
    private final String oldAvatar;
    private final String avatar;
    private final UUID userId;
    private final String username;

    /**
     * Construct.
     */
    public UserAvatarUpdateEvent(Object source,
                                 String oldAvatar, String avatar, UUID userId, String username) {
        super(source);
        this.oldAvatar = oldAvatar;
        this.avatar = avatar;
        this.userId = userId;
        this.username = username;
    }

}
