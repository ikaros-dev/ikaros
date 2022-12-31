package run.ikaros.server.crd.router;

import jakarta.annotation.Nonnull;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.server.crd.scheme.CRDScheme;
import run.ikaros.server.crd.store.CRDStoreClient;

/**
 * @author: li-guohao
 */
public class CRDRouterFunctionFactory {
    private final CRDScheme scheme;
    private final CRDStoreClient storeClient;

    public CRDRouterFunctionFactory(CRDScheme scheme, CRDStoreClient storeClient) {
        this.scheme = scheme;
        this.storeClient = storeClient;
    }

    @Nonnull
    public RouterFunction<ServerResponse> build() {

        return null;
    }



}
