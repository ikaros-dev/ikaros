package run.ikaros.server.core.role.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.RoleEntity;

class RoleEventTest {

    private RoleEntity buildRoleEntity() {
        RoleEntity entity = RoleEntity.builder()
            .name("testRole")
            .description("A test role")
            .build();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    @Test
    void roleCreatedEventGetters() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        RoleCreatedEvent event = new RoleCreatedEvent(source, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
    }

    @Test
    void roleCreatedEventWithClock() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        RoleCreatedEvent event = new RoleCreatedEvent(source, clock, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
    }

    @Test
    void roleRemoveEventGetters() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        RoleRemoveEvent event = new RoleRemoveEvent(source, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
    }

    @Test
    void roleRemoveEventWithClock() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        RoleRemoveEvent event = new RoleRemoveEvent(source, clock, roleEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
    }

    @Test
    void roleRelationCreatedEventGetters() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        Long userId = 12345L;
        RoleRelationCreatedEvent event = new RoleRelationCreatedEvent(source, roleEntity, userId);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
        assertEquals(userId, event.getUserId());
    }

    @Test
    void roleRelationCreatedEventWithClock() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        Long userId = 12345L;
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        RoleRelationCreatedEvent event =
            new RoleRelationCreatedEvent(source, clock, roleEntity, userId);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
        assertEquals(userId, event.getUserId());
    }

    @Test
    void roleRelationRemoveEventGetters() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        Long userId = 67890L;
        RoleRelationRemoveEvent event = new RoleRelationRemoveEvent(source, roleEntity, userId);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
        assertEquals(userId, event.getUserId());
    }

    @Test
    void roleRelationRemoveEventWithClock() {
        Object source = new Object();
        RoleEntity roleEntity = buildRoleEntity();
        Long userId = 67890L;
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        RoleRelationRemoveEvent event =
            new RoleRelationRemoveEvent(source, clock, roleEntity, userId);

        assertSame(source, event.getSource());
        assertNotNull(event.getRoleEntity());
        assertEquals(roleEntity, event.getRoleEntity());
        assertEquals(userId, event.getUserId());
    }

    @Test
    void roleCreatedEventIsApplicationEvent() {
        RoleCreatedEvent event = new RoleCreatedEvent(new Object(), buildRoleEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void roleRemoveEventIsApplicationEvent() {
        RoleRemoveEvent event = new RoleRemoveEvent(new Object(), buildRoleEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void roleRelationCreatedEventIsApplicationEvent() {
        RoleRelationCreatedEvent event =
            new RoleRelationCreatedEvent(new Object(), buildRoleEntity(), 1L);
        assertNotNull(event.getTimestamp());
    }

    @Test
    void roleRelationRemoveEventIsApplicationEvent() {
        RoleRelationRemoveEvent event =
            new RoleRelationRemoveEvent(new Object(), buildRoleEntity(), 1L);
        assertNotNull(event.getTimestamp());
    }
}
