package run.ikaros.server.store.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.AttachmentRelationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "attachment_relation")
public class AttachmentRelationEntity {
    @Id
    private Long id;
    @Column("attachment_id")
    private Long attachmentId;
    private AttachmentRelationType type;
    @Column("relation_attachment_id")
    private Long relationAttachmentId;
    private UUID uuid;
}
