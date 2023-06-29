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
@Table(name = "file_remote")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FileRemoteEntity extends BaseEntity {
    @Column("file_id")
    private Long fileId;
    @Column("remote_id")
    private String remoteId;
    private String remote;
    private String md5;
    @Column("file_name")
    private String fileName;
    private String path;
    @Column("file_size")
    private Long size;
}
