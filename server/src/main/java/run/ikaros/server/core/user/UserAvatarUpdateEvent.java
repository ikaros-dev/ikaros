package run.ikaros.server.core.user;

import java.util.UUID;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserAvatarUpdateEvent extends ApplicationEvent {
    private final @Nullable String oldAvatar;
    private final @Nullable String avatar;
    private final @Nullable UUID userId;
    private final @Nullable String username;

    /**
     * Construct.
     */
    public UserAvatarUpdateEvent(Object source,
                                 @Nullable String oldAvatar, @Nullable String avatar,
                                 @Nullable UUID userId, @Nullable String username) {
        super(source);
        this.oldAvatar = oldAvatar;
        this.avatar = avatar;
        this.userId = userId;
        this.username = username;
    }

}
