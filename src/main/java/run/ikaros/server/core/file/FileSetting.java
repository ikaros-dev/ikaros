package run.ikaros.server.core.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.Name;
import run.ikaros.server.infra.constant.OpenApiConst;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Custom(group = OpenApiConst.FILE_GROUP, version = OpenApiConst.FILE_VERSION,
    kind = "FileSetting", singular = "setting", plural = "settings")
public class FileSetting {
    @Name
    private String policy;
}
