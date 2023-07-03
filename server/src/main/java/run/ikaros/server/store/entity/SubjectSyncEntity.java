package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject_sync")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SubjectSyncEntity extends BaseEntity {
    @Column("subject_id")
    private Long subjectId;

    @Column("platform")
    private SubjectSyncPlatform platform;

    @Column("platform_id")
    private String platformId;

    @Column("sync_time")
    private LocalDateTime syncTime;
}
