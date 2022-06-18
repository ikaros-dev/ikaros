package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.config.EntityAuditorConfig;
import cn.liguohao.ikaros.entity.RelationEntity;
import cn.liguohao.ikaros.enums.Role;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author li-guohao
 * @date 2022/06/04
 */
@SpringBootTest
class RelationServiceTest {

    @Autowired
    RelationService relationService;

    private static final Long NOT_LOGIN_MASTER_UID = EntityAuditorConfig.UUID_WHEN_NO_AUTH;
    private static final Long MASTER_UID = NOT_LOGIN_MASTER_UID;
    private static final Long GUEST_UID = 1L;
    private static final Role ROLE = Role.ATTRACTOR;


    @BeforeEach
    void setUp() {

    }

    @Test
    void findByMasterAndGuestUidAndRole() {
        RelationEntity relationEntity =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, ROLE);
        Assertions.assertNull(relationEntity);

        relationService.save(MASTER_UID, GUEST_UID, ROLE);

        RelationEntity relationEntityAfterSave =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, ROLE);
        Assertions.assertNotNull(relationEntityAfterSave);
        Assertions.assertEquals(relationEntityAfterSave.getRole(), ROLE);
        Assertions.assertEquals(relationEntityAfterSave.getMasterUid(), MASTER_UID);
        Assertions.assertEquals(relationEntityAfterSave.getGuestUid(), GUEST_UID);

        relationService.delete(MASTER_UID, GUEST_UID, ROLE);

        RelationEntity relationEntityAfterDelete =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, ROLE);
        Assertions.assertNull(relationEntityAfterDelete);
    }

    @Test
    void findRoleByMasterAndGuestUid() {
        List<Role> roles = relationService.findRoleByMasterAndGuestUid(MASTER_UID, GUEST_UID);
        Assertions.assertNotNull(roles);
        Assertions.assertTrue(roles.isEmpty());

        relationService.save(MASTER_UID, GUEST_UID, ROLE);

        List<Role> rolesAfterSave =
            relationService.findRoleByMasterAndGuestUid(MASTER_UID, GUEST_UID);
        Assertions.assertNotNull(rolesAfterSave);
        Assertions.assertEquals(1, rolesAfterSave.size());

        relationService.delete(MASTER_UID, GUEST_UID, ROLE);

        List<Role> rolesAfterDelete =
            relationService.findRoleByMasterAndGuestUid(MASTER_UID, GUEST_UID);
        Assertions.assertNotNull(rolesAfterDelete);
        Assertions.assertTrue(rolesAfterDelete.isEmpty());
    }

    @Test
    void saveAndDelete() {
        relationService.save(MASTER_UID, GUEST_UID, ROLE);

        List<Role> roles = relationService.findRoleByMasterAndGuestUid(MASTER_UID, GUEST_UID);
        Assertions.assertNotNull(roles);
        Assertions.assertEquals(1, roles.size());

        Role role = roles.get(0);
        Assertions.assertEquals(ROLE, role);

        relationService.delete(MASTER_UID, GUEST_UID, ROLE);

        List<Role> rolesAfterDelete =
            relationService.findRoleByMasterAndGuestUid(MASTER_UID, GUEST_UID);
        Assertions.assertNotNull(rolesAfterDelete);
        Assertions.assertTrue(rolesAfterDelete.isEmpty());
    }


    @Test
    void follow() {
        Role attractor = Role.ATTRACTOR;

        RelationEntity relationEntity =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, attractor);
        Assertions.assertNull(relationEntity);

        relationService.follow(GUEST_UID);

        RelationEntity relationEntityAfterFollow =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, attractor);
        Assertions.assertNotNull(relationEntityAfterFollow);
        Assertions.assertEquals(relationEntityAfterFollow.getRole(), attractor);
        Assertions.assertEquals(relationEntityAfterFollow.getMasterUid(), MASTER_UID);
        Assertions.assertEquals(relationEntityAfterFollow.getGuestUid(), GUEST_UID);

        relationService.delete(MASTER_UID, GUEST_UID, attractor);

        RelationEntity relationEntityAfterDelete =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, Role.ATTRACTOR);
        Assertions.assertNull(relationEntityAfterDelete);

    }

    @Test
    void unFollow() {
        // 测试未关注时调用 取消关注 时是否会抛出异常导致失败
        relationService.unFollow(GUEST_UID);

        Role attractor = Role.ATTRACTOR;

        RelationEntity relationEntity =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, attractor);
        Assertions.assertNull(relationEntity);

        relationService.follow(GUEST_UID);

        RelationEntity relationEntityAfterFollow =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, Role.ATTRACTOR);
        Assertions.assertNotNull(relationEntityAfterFollow);
        Assertions.assertEquals(relationEntityAfterFollow.getRole(), attractor);
        Assertions.assertEquals(relationEntityAfterFollow.getMasterUid(), MASTER_UID);
        Assertions.assertEquals(relationEntityAfterFollow.getGuestUid(), GUEST_UID);

        relationService.unFollow(GUEST_UID);

        RelationEntity relationEntityAfterDelete =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, Role.ATTRACTOR);
        Assertions.assertNull(relationEntityAfterDelete);
    }


    @Test
    void beFollowed() {
        Role fan = Role.FAN;

        RelationEntity relationEntity =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, fan);
        Assertions.assertNull(relationEntity);

        relationService.beFollowed(GUEST_UID);

        RelationEntity relationEntityAfterFollow =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, fan);
        Assertions.assertNotNull(relationEntityAfterFollow);
        Assertions.assertEquals(relationEntityAfterFollow.getRole(), fan);
        Assertions.assertEquals(relationEntityAfterFollow.getMasterUid(), MASTER_UID);
        Assertions.assertEquals(relationEntityAfterFollow.getGuestUid(), GUEST_UID);

        relationService.delete(MASTER_UID, GUEST_UID, fan);

        RelationEntity relationEntityAfterDelete =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, Role.ATTRACTOR);
        Assertions.assertNull(relationEntityAfterDelete);
    }


    @Test
    void beUnFollowed() {
        // 测试未关注时调用 被取消关注 时是否会抛出异常导致失败
        relationService.beUnFollowed(GUEST_UID);

        Role fan = Role.FAN;

        RelationEntity relationEntity =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, fan);
        Assertions.assertNull(relationEntity);

        relationService.beFollowed(GUEST_UID);

        RelationEntity relationEntityAfterFollow =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, fan);
        Assertions.assertNotNull(relationEntityAfterFollow);
        Assertions.assertEquals(relationEntityAfterFollow.getRole(), fan);
        Assertions.assertEquals(relationEntityAfterFollow.getMasterUid(), MASTER_UID);
        Assertions.assertEquals(relationEntityAfterFollow.getGuestUid(), GUEST_UID);

        relationService.beUnFollowed(GUEST_UID);

        RelationEntity relationEntityAfterDelete =
            relationService.findByMasterAndGuestUidAndRole(MASTER_UID, GUEST_UID, fan);
        Assertions.assertNull(relationEntityAfterDelete);
    }
}