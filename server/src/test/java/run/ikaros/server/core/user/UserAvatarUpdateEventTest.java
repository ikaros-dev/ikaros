package run.ikaros.server.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserAvatarUpdateEventTest {

    @Test
    void constructorAndGetters() {
        Object source = new Object();
        String oldAvatar = "https://example.com/old-avatar.png";
        String avatar = "https://example.com/new-avatar.png";
        UUID userId = UUID.randomUUID();
        String username = "testUser";

        UserAvatarUpdateEvent event =
            new UserAvatarUpdateEvent(source, oldAvatar, avatar, userId, username);

        assertSame(source, event.getSource());
        assertEquals(oldAvatar, event.getOldAvatar());
        assertEquals(avatar, event.getAvatar());
        assertEquals(userId, event.getUserId());
        assertEquals(username, event.getUsername());
    }

    @Test
    void isApplicationEvent() {
        UserAvatarUpdateEvent event = new UserAvatarUpdateEvent(
            new Object(), "old", "new", UUID.randomUUID(), "user");
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullFields() {
        Object source = new Object();
        UserAvatarUpdateEvent event =
            new UserAvatarUpdateEvent(source, null, null, null, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getOldAvatar());
        assertEquals(null, event.getAvatar());
        assertEquals(null, event.getUserId());
        assertEquals(null, event.getUsername());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        UserAvatarUpdateEvent event1 =
            new UserAvatarUpdateEvent(source, "old1", "new1", userId1, "user1");
        final UserAvatarUpdateEvent event2 =
            new UserAvatarUpdateEvent(source, "old2", "new2", userId2, "user2");

        assertEquals("old1", event1.getOldAvatar());
        assertEquals("new1", event1.getAvatar());
        assertEquals(userId1, event1.getUserId());
        assertEquals("user1", event1.getUsername());
        assertEquals("old2", event2.getOldAvatar());
        assertEquals("new2", event2.getAvatar());
        assertEquals(userId2, event2.getUserId());
        assertEquals("user2", event2.getUsername());
    }

    @Test
    void constructorWithEmptyStrings() {
        Object source = new Object();
        UUID userId = UUID.randomUUID();

        UserAvatarUpdateEvent event =
            new UserAvatarUpdateEvent(source, "", "", userId, "");

        assertEquals("", event.getOldAvatar());
        assertEquals("", event.getAvatar());
        assertEquals(userId, event.getUserId());
        assertEquals("", event.getUsername());
    }
}
