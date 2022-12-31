package run.ikaros.server.crd.router.define;

import run.ikaros.server.crd.scheme.CRDScheme;

/**
 * @author: li-guohao
 */
public interface ApiPathPatternGenerator {

    String pathPattern();

    default String buildCRDPathPattern(CRDScheme scheme) {
        var gvk = scheme.groupVersionKind();
        StringBuilder pattern = new StringBuilder();
        if (gvk.hasGroup()) {
            pattern.append("/apis/").append(gvk.group());
        } else {
            pattern.append("/api");
        }
        return pattern.append('/').append(gvk.version()).append('/').append(scheme.plural())
            .toString();
    }
}
