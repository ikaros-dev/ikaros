package run.ikaros.server.crd;

import lombok.Data;

/**
 * @author: li-guohao
 */
@Data
public abstract class AbstractCustomResourceDefinition implements CustomResourceDefinition {
    private String apiVersion;

    private String kind;

    private Metadata metadata;

    @Override
    public String getApiVersion() {
        var apiVersionFromGvk = CustomResourceDefinition.super.getApiVersion();
        return apiVersionFromGvk != null ? apiVersionFromGvk : this.apiVersion;
    }

    @Override
    public String getKind() {
        var kindFromGvk = CustomResourceDefinition.super.getKind();
        return kindFromGvk != null ? kindFromGvk : this.kind;
    }
}
