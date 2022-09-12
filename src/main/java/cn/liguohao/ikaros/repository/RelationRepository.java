package cn.liguohao.ikaros.repository;

import cn.liguohao.ikaros.model.entity.RelationEntity;
import cn.liguohao.ikaros.model.enums.Role;
import java.util.List;

/**
 * @author li-guohao
 * @date 2022/06/03
 */
public interface RelationRepository extends BaseRepository<RelationEntity> {

    /**
     * 查询是否存在对应的主体和客体的关系
     *
     * @param masterUid 主体ID
     * @param guestUid  客体ID
     * @param role      主客体之间的关系
     * @return 关系记录
     */
    RelationEntity findByMasterUidAndGuestUidAndRole(Long masterUid, Long guestUid, Role role);

    /**
     * 查询是否存在对应的主体和客体的关系
     *
     * @param masterUid 主体ID
     * @param guestUid  客体ID
     * @return 关系记录集合
     */
    List<RelationEntity> findByMasterUidAndGuestUid(Long masterUid, Long guestUid);
}
