package run.ikaros.api.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
@TableName("subject_person")
public class SubjectPerson implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private LocalDateTime createTime;

    private Object createUid;

    private Boolean deleteStatus;

    private LocalDateTime updateTime;

    private UUID updateUid;

    private Long olVersion;

    private UUID subjectId;

    private UUID personId;
}
