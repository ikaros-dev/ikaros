package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 记录可重建派生附件与其原始来源附件的追溯关系。 */
@Table("derived_attachment")
public record DerivedAttachmentEntity(@Id UUID id, @Column("source_attachment_id") UUID sourceAttachmentId,
                                      @Column("derived_attachment_id") UUID derivedAttachmentId,
                                      @Column("created_at") Instant createdAt, @Version Long version) { }
