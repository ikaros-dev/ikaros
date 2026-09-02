package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Resource 对 Blob 的业务引用，负责表达文件名和附件角色，不保存物理路径。
 */
@Table("attachment")
public record AttachmentEntity(
    @Id UUID id,
    @Column("resource_id") UUID resourceId,
    @Column("blob_id") UUID blobId,
    @Column("file_name") String fileName,
    @Column("attachment_kind") AttachmentKind attachmentKind,
    @Column("created_at") Instant createdAt,
    @Column("deleted_at") Instant deletedAt,
    @Version Long version
) {
}
