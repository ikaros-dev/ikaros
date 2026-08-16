package run.ikaros.api.search.subject;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.SubjectType;

@Data
public class SubjectDoc {
    private @Nullable UUID id;
    private @Nullable SubjectType type;
    private @Nullable String name;
    private @Nullable String nameCn;
    private @Nullable String infobox;
    private @Nullable String summary;
    private @Nullable Boolean nsfw;
    private @Nullable Long airTime;
    private @Nullable String cover;
    private @Nullable List<String> tags;
}
