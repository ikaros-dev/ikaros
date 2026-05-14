package run.ikaros.server.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import run.ikaros.api.store.entity.UserRole;

public interface UserRoleMapper extends BaseMapper<UserRole> {
    @Select("select * from ikuser_role where user_id = #{userId}")
    UserRole findByUserId(Long userId);
}
