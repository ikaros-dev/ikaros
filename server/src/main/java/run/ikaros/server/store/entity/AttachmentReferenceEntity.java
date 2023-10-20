package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.AttachmentReferenceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "attachment_reference")
public class AttachmentReferenceEntity {
    @Id
    private Long id;
    private AttachmentReferenceType type;
    @Column("attachment_id")
    private Long attachmentId;
    @Column("reference_id")
    private Long referenceId;
}
