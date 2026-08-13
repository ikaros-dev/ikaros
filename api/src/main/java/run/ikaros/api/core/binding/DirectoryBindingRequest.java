package run.ikaros.api.core.binding;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DirectoryBindingRequest {
    private @Nullable UUID directoryId;
    private @Nullable SubjectSyncPlatform platform;
}
