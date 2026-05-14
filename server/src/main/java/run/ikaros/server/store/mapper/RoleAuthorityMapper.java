package run.ikaros.server.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Select;
import run.ikaros.api.store.entity.RoleAuthority;

public interface RoleAuthorityMapper extends BaseMapper<RoleAuthority> {
    @Select("select * from role_authority where role_id = #{roleId}")
    List<RoleAuthority> findAllByRoleId(Long roleId);
}
