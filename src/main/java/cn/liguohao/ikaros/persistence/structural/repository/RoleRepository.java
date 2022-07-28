package cn.liguohao.ikaros.persistence.structural.repository;


import cn.liguohao.ikaros.persistence.structural.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author li-guohao
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

}
