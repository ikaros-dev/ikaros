package run.ikaros.api.core.authority;


import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.AuthorityType;

@Data
@Builder
public class AuthorityCondition {
    private Boolean allow;
    private AuthorityType type;
    private String target;
    private String authority;
    private Integer page;
    private Integer size;
}
