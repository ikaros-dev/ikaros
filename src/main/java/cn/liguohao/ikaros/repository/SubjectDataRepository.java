package cn.liguohao.ikaros.repository;

import cn.liguohao.ikaros.entity.ItemDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
public interface SubjectDataRepository extends JpaRepository<ItemDataEntity, Long> {
}
