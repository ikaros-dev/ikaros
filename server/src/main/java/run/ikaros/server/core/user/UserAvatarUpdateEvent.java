package run.ikaros.server.core.user;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserAvatarUpdateEvent extends ApplicationEvent {
    private final String oldAvatar;
    private final String avatar;
    private final Long userId;
    private final String username;

    /**
     * Construct.
     */
    public UserAvatarUpdateEvent(Object source,
                                 String oldAvatar, String avatar, Long userId, String username) {
        super(source);
        this.oldAvatar = oldAvatar;
        this.avatar = avatar;
        this.userId = userId;
        this.username = username;
    }

}
