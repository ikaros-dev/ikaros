package run.ikaros.server.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import run.ikaros.server.store.entity.UserRole;

import java.util.UUID;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author chivehao
 * @since 2026-05-13
 */
public interface UserRoleMapper extends BaseMapper<UserRole> {
    @Select("select * from ikuser_role where user_id = #{userId}")
    UserRole findByUserId(UUID userId);
}
