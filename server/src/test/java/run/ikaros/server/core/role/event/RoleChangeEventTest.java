package run.ikaros.server.core.role.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.RoleEntity;

class RoleChangeEventTest {

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

        RoleChangeEvent event = new RoleChangeEvent(source, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
    }

    @Test
    void constructorWithClock() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

        RoleChangeEvent event = new RoleChangeEvent(source, clock, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
        assertEquals(clock.millis(), event.getTimestamp());
    }

    @Test
    void isApplicationEvent() {
        RoleChangeEvent event = new RoleChangeEvent(new Object(), buildRoleEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullEntity() {
        Object source = new Object();
        RoleChangeEvent event = new RoleChangeEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getRoleEntity());
    }

    @Test
    void constructorWithClockAndNullEntity() {
        Object source = new Object();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));
        RoleChangeEvent event = new RoleChangeEvent(source, clock, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getRoleEntity());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        RoleEntity roleEntity1 = buildRoleEntity();
        RoleEntity roleEntity2 = buildRoleEntity();

        RoleChangeEvent event1 = new RoleChangeEvent(source, roleEntity1);
        RoleChangeEvent event2 = new RoleChangeEvent(source, roleEntity2);

        assertEquals(roleEntity1, event1.getRoleEntity());
        assertEquals(roleEntity2, event2.getRoleEntity());
        assertSame(event1.getRoleEntity(), roleEntity1);
        assertSame(event2.getRoleEntity(), roleEntity2);
    }
}
