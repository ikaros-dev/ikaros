package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.entity.BaseEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "folder")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FolderEntity extends BaseEntity {
    @Column("parent_id")
    private Long parentId;
    private String name;
}
