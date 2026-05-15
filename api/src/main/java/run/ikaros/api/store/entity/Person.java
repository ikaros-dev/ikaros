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
public class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private LocalDateTime createTime;

    private UUID createUid;

    private Boolean deleteStatus;

    private LocalDateTime updateTime;

    private UUID updateUid;

    private Long olVersion;

    private String name;

    private String infobox;

    private String summary;
}
