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
@TableName("person_character")
public class PersonCharacter implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private LocalDateTime createTime;

    private Object createUid;

    private Boolean deleteStatus;

    private LocalDateTime updateTime;

    private Object updateUid;

    private Long olVersion;

    private Object personId;

    private Object characterId;
}
