package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.server.store.enums.SubjectType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SubjectEntity extends BaseEntity {
    /**
     * Subject type.
     *
     * @see SubjectType#getCode()
     */
    private Integer type;
    private String name;
    @Column("name_cn")
    private String nameCn;
    private String infobox;
    private String platform;
    private String summary;
    /**
     * Can search by anonymous access.
     */
    private Boolean nsfw;
}
