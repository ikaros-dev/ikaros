package run.ikaros.server.subject.store.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * SubjectEntity is an entity for storing Subject data into database.
 *
 * @author: li-guohao
 */
@Data
@Table("subject")
public class SubjectEntity {
    @Id
    private Long id;
    @Column("parent_id")
    private Long parentId;
    /**
     * This field only for serving optimistic lock value.
     */
    @Version
    private Long version;
    private String group;
    @Column("api_version")
    private String apiVersion;
    private String kind;
    private String plural;
    private String singular;
    private String name;
    private String url;
}
