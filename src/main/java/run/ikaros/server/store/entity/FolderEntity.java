package run.ikaros.server.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * a container for store file logically.
 *
 * @author liguohao
 */
@Data
@Entity
@Table(name = "folder")
@EqualsAndHashCode(callSuper = true)
public class FolderEntity extends BaseEntity {

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    /**
     * folder name.
     */
    @Column(nullable = false)
    private String name;

}