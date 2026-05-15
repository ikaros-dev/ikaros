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
@TableName("subject_collection")
public class SubjectCollection implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private UUID userId;

    private UUID subjectId;

    private String type;

    private Long mainEpProgress;

    private Boolean isPrivate;

    private String comment;

    private Long score;
}
