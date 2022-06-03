package cn.liguohao.ikaros.service;

import static cn.liguohao.ikaros.service.UserService.getCurrentLoginUser;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.exceptions.service.RelationNotExistException;
import cn.liguohao.ikaros.define.enums.Role;
import cn.liguohao.ikaros.entity.RelationEntity;
import cn.liguohao.ikaros.entity.UserEntity;
import cn.liguohao.ikaros.repository.RelationRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 关系围绕主体为中心
 *
 * @author li-guohao
 * @date 2022/06/03
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class RelationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationService.class);
    private final RelationRepository relationRepository;

    public RelationService(RelationRepository relationRepository) {
        this.relationRepository = relationRepository;
    }

    /**
     * @return 当前登录用户(主体)的UID
     */
    private Long getCurrentLoginUserId() {
        UserEntity master = getCurrentLoginUser();
        return Objects.requireNonNull(master).getId();
    }

    /**
     * 保存主体客体之间的关系，已存在则不操作，不存在则新增
     *
     * @param masterUid 主体Uid
     * @param guestUid  客体Uid
     * @param role      主客体间的关系
     * @return 保存后的实体对象
     */
    public RelationEntity save(Long masterUid, Long guestUid, Role role) {
        Assert.isNotNull(masterUid, guestUid, role);

        RelationEntity relationEntity =
            relationRepository.findByMasterIdAndGuestIdAndRole(masterUid, guestUid, role);

        if (null == relationEntity) {
            relationEntity = new RelationEntity()
                .setRole(role)
                .setMasterUid(masterUid)
                .setGuestUid(guestUid);
            relationEntity = relationRepository.save(relationEntity);
            LOGGER.debug("add new relation, relation:{}", relationEntity);
        }

        return relationEntity;
    }

    /**
     * @return 保存后的实体对象
     * @see #save(Long, Long, Role)
     */
    private RelationEntity save(Long guestUid, Role role) {
        Assert.isNotNull(guestUid, role);

        Long masterUid = getCurrentLoginUserId();

        return save(masterUid, guestUid, role);
    }

    /**
     * 移除主体客体之间的关系，已存在则移除，不存在则不操作
     *
     * @param masterUid 主体Uid
     * @param guestUid  客体Uid
     * @param role      主客体间的关系
     */
    public void delete(Long masterUid, Long guestUid, Role role) {
        Assert.isNotNull(masterUid, guestUid, role);

        RelationEntity relationEntity =
            relationRepository.findByMasterIdAndGuestIdAndRole(masterUid, guestUid, role);

        if (relationEntity != null) {
            relationRepository.delete(relationEntity);
            LOGGER.debug("delete exist relation, relation:{}", relationEntity);
        }
    }

    /**
     * @see #delete(Long, Long, Role)
     */
    public void delete(Long guestUid, Role role) {
        Assert.isNotNull(guestUid, role);

        Long masterUid = getCurrentLoginUserId();

        delete(masterUid, guestUid, role);
    }


    /**
     * 关注
     *
     * @param guestUid 客体UID
     */
    public void follow(Long guestUid) {
        Assert.isNotNull(guestUid);

        save(guestUid, Role.ATTRACTOR);
    }


    /**
     * 被关注
     *
     * @param guestUid 客体UID
     */
    public void beFollowed(Long guestUid) {
        Assert.isNotNull(guestUid);

        save(guestUid, Role.FAN);
    }

    /**
     * 根据主体UID和客体UID查询关系类型
     *
     * @param masterUid 主体UID
     * @param guestUid  客体UID
     * @return 主体客体之间的关系
     */
    public List<Role> findRoleByMasterAndGuestUid(
        Long masterUid, Long guestUid)
        throws RelationNotExistException {
        Assert.isNotNull(masterUid, guestUid);

        return relationRepository
            .findByMasterIdAndGuestId(masterUid, guestUid)
            .stream()
            .flatMap(
                (Function<RelationEntity, Stream<Role>>) relationEntity
                    -> Stream.of(relationEntity.role()))
            .collect(Collectors.toList());

    }


    /**
     * 取消关注
     *
     * @param guestUid 客体UID
     */
    public void unFollow(Long guestUid) {
        Assert.isNotNull(guestUid);

        delete(guestUid, Role.ATTRACTOR);
    }

    /**
     * 被取消关注
     *
     * @param guestUid 客体UID
     */
    public void beUnFollowed(Long guestUid) {
        Assert.isNotNull(guestUid);

        delete(guestUid, Role.FAN);
    }


}
