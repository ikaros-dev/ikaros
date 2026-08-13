package run.ikaros.api.core.authority;


import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AuthorityType;

@Data
@Builder
public class AuthorityCondition {
    private @Nullable Boolean allow;
    private @Nullable AuthorityType type;
    private @Nullable String target;
    private @Nullable String authority;
    private @Nullable Integer page;
    private @Nullable Integer size;
}
