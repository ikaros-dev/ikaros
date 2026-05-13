package run.ikaros.server.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author chivehao
 * @since 2026-05-13
 */
@Getter
@Setter
@ToString
@TableName("attachment_driver")
public class AttachmentDriver implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private Boolean enable;

    private String dType;

    private String dName;

    private String mountName;

    private String remotePath;

    private Long dOrder;

    private String dComment;

    private String refreshToken;

    private String accessToken;

    private LocalDateTime expireTime;

    private Long listPageSize;

    private String rootDirId;

    private Long requestLimit;

    private String userId;

    private String userName;

    private String avatar;

    private Long spaceTotal;

    private Long spaceUse;
}
