package run.ikaros.server.crd;

/**
 * @author: li-guohao
 */
public record GroupVersionKind(String group, String version, String kind) {
    public GroupVersion groupVersion() {
        return new GroupVersion(group, version);
    }
}
