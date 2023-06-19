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
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.store.entity.BaseEntity;
import run.ikaros.api.store.enums.SubjectType;

/**
 * Subject entity.
 *
 * @see run.ikaros.server.core.subject.SubjectService#update(Subject)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SubjectEntity extends BaseEntity {
    private SubjectType type;
    private String name;
    @Column("name_cn")
    private String nameCn;
    private String infobox;
    private String summary;
    /**
     * Not Safe/Suitable For Work.
     */
    private Boolean nsfw;
    private String cover;
    @Column("air_time")
    private LocalDateTime airTime;
}
