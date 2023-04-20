package run.ikaros.server.core.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.custom.Name;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Custom(group = OpenApiConst.FILE_GROUP, version = OpenApiConst.FILE_VERSION,
    kind = "FilePolicy", singular = "policy", plural = "policies")
public class FilePolicy {
    @Name
    private String name;
}
