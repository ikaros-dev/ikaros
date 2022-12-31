package run.ikaros.server.crd;

import org.springframework.util.StringUtils;
import run.ikaros.server.crd.router.define.ApiPathPatternGenerator;
import run.ikaros.server.crd.scheme.CRDScheme;

/**
 * @author: li-guohao
 */
public record GroupVersionKind(String group, String version, String kind) {
    public GroupVersion groupVersion() {
        return new GroupVersion(group, version);
    }

    /**
     * @return Group Version Kind has group, if true is core api, false is crd apis
     * @see ApiPathPatternGenerator#buildExtensionPathPattern(CRDScheme)
     */
    public boolean hasGroup() {
        return StringUtils.hasText(group);
    }
}
