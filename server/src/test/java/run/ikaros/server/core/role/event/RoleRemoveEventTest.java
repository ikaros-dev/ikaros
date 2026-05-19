package run.ikaros.server.core.role.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.RoleEntity;

class RoleRemoveEventTest {

    private RoleEntity buildRoleEntity() {
        RoleEntity entity = RoleEntity.builder()
            .name("testRole")
            .description("A test role")
            .build();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    @Test
    void constructorWithSourceAndRoleEntity() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();

        RoleRemoveEvent event = new RoleRemoveEvent(source, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
    }

    @Test
    void constructorWithClock() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

        RoleRemoveEvent event = new RoleRemoveEvent(source, clock, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
        assertEquals(clock.millis(), event.getTimestamp());
    }

    @Test
    void isApplicationEvent() {
        RoleRemoveEvent event = new RoleRemoveEvent(new Object(), buildRoleEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void isRoleChangeEvent() {
        RoleRemoveEvent event = new RoleRemoveEvent(new Object(), buildRoleEntity());
        assertTrue(event instanceof RoleChangeEvent);
    }

    @Test
    void constructorWithNullEntity() {
        Object source = new Object();
        RoleRemoveEvent event = new RoleRemoveEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getRoleEntity());
    }

    @Test
    void constructorWithClockAndNullEntity() {
        Object source = new Object();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));
        RoleRemoveEvent event = new RoleRemoveEvent(source, clock, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getRoleEntity());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        RoleEntity roleEntity1 = buildRoleEntity();
        RoleEntity roleEntity2 = buildRoleEntity();

        RoleRemoveEvent event1 = new RoleRemoveEvent(source, roleEntity1);
        RoleRemoveEvent event2 = new RoleRemoveEvent(source, roleEntity2);

        assertEquals(roleEntity1, event1.getRoleEntity());
        assertEquals(roleEntity2, event2.getRoleEntity());
        assertSame(event1.getRoleEntity(), roleEntity1);
        assertSame(event2.getRoleEntity(), roleEntity2);
    }
}
