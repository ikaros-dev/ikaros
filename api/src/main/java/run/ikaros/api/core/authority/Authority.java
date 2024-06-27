package run.ikaros.api.core.authority;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.AuthorityType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Authority {
    private Long id;
    private Boolean allow;
    private AuthorityType type;
    private String target;
    private String authority;
}
