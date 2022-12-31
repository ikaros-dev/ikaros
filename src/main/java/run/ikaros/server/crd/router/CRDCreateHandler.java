package run.ikaros.server.crd.router;

import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.server.crd.CRDClient;
import run.ikaros.server.crd.router.define.CreateHandler;
import run.ikaros.server.crd.scheme.CRDScheme;

/**
 * @author: li-guohao
 */
public class CRDCreateHandler implements CreateHandler {

    private final CRDScheme scheme;
    private final CRDClient client;

    public CRDCreateHandler(CRDScheme scheme, CRDClient client) {
        this.scheme = scheme;
        this.client = client;
    }


    @Override
    public ServerResponse handle(ServerRequest request) throws Exception {

        return null;
    }

    @Override
    public String pathPattern() {
        return buildCRDPathPattern(scheme);
    }
}
