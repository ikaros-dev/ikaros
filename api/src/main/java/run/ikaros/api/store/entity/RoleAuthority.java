package run.ikaros.api.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("role_authority")
public class RoleAuthority implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private UUID roleId;

    private UUID authorityId;
}
