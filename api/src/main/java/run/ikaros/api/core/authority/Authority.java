package run.ikaros.api.core.authority;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AuthorityType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Authority {
    private @Nullable UUID id;
    private @Nullable Boolean allow;
    private @Nullable AuthorityType type;
    private @Nullable String target;
    private @Nullable String authority;
}
