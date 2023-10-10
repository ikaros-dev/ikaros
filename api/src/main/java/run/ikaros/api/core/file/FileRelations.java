package run.ikaros.api.core.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.FileRelationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileRelations {
    private Long id;
    @JsonProperty("file_id")
    private Long fileId;
    @JsonProperty("relation_type")
    private FileRelationType relationType;
    @JsonProperty("relation_file_ids")
    private List<Long> relationFileIds;
}
