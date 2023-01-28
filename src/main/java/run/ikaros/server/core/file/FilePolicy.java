package run.ikaros.server.core.file;

import lombok.Data;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.infra.constant.OpenApiConst;

@Data
@Custom(group = OpenApiConst.CORE_GROUP, version = OpenApiConst.CORE_VERSION,
    kind = "FilePolicy", singular = "file-policy", plural = "file-policy")
public class FilePolicy {
    private Integer storageStrategy;
}
