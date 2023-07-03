package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.CollectionStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "collection")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CollectionEntity extends BaseEntity {
    @Column("user_id")
    private Long userId;
    @Column("subject_id")
    private Long subjectId;
    /**
     * collection status.
     *
     * @see CollectionStatus#getCode()
     */
    private Integer status;
}
