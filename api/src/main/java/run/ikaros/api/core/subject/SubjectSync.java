package run.ikaros.api.core.subject;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SubjectSync {
    private UUID id;
    private UUID subjectId;
    private SubjectSyncPlatform platform;
    private String platformId;
    private LocalDateTime syncTime;
}
