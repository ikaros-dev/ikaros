package cn.liguohao.ikaros.repository;

import cn.liguohao.ikaros.define.enums.Role;
import cn.liguohao.ikaros.entity.RelationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author li-guohao
 * @date 2022/06/03
 */
public interface RelationRepository extends JpaRepository<RelationEntity, Long> {

    /**
     * 查询是否存在对应的主体和客体的关系
     *
     * @param masterId 主体ID
     * @param guestId  客体ID
     * @param role 主客体之间的关系
     * @return 关系记录
     */
    RelationEntity findByMasterIdAndGuestIdAndRole(Long masterId, Long guestId, Role role);

    /**
     * 查询是否存在对应的主体和客体的关系
     *
     * @param masterId 主体ID
     * @param guestId  客体ID
     * @return 关系记录集合
     */
    List<RelationEntity> findByMasterIdAndGuestId(Long masterId, Long guestId);
}
