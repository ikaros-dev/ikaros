package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.FileRelationType;

@Data
@Table(name = "file_relation")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileRelationEntity {
    @Id
    private Long id;
    @Column("file_id")
    private Long fileId;
    @Column("relation_type")
    private FileRelationType relationType;
    @Column("relation_file_id")
    private Long relationFileId;
}
