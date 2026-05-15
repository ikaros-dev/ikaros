package run.ikaros.api.store.entity;

import java.io.Serializable;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private String type;

    private UUID masterId;

    private String name;

    private UUID userId;

    private LocalDateTime createTime;

    private String color;
}
