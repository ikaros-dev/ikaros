package run.ikaros.api.core.person;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Person {
    private @Nullable UUID id;
    private @Nullable String name;
    private @Nullable String infobox;
    private @Nullable String summary;
}
