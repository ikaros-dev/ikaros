package run.ikaros.server.crd;

import run.ikaros.server.crd.store.CRDStoreClient;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author: li-guohao
 */
public class DefaultCRDClient implements CRDClient {

    private final CRDStoreClient storeClient;

    public DefaultCRDClient(CRDStoreClient storeClient) {
        this.storeClient = storeClient;
    }

    @Override
    public <C extends CustomResourceDefinition> List<C> list(Class<C> type, Predicate<C> predicate,
                                                             Comparator<C> comparator) {
        // todo impl search by predicate
        return null;
    }

    @Override
    public <C extends CustomResourceDefinition> PageListResult<C> list(Class<C> type,
                                                                       Predicate<C> predicate,
                                                                       Comparator<C> comparator,
                                                                       int page, int size) {
        // todo impl search by predicate
        return null;
    }

    @Override
    public <C extends CustomResourceDefinition> Optional<C> fetch(Class<C> type, String name) {
        return Optional.empty();
    }

    @Override
    public Optional<CustomResourceDefinition> fetch(GroupVersionKind gvk, String name) {
        return Optional.empty();
    }

    @Override
    public <C extends CustomResourceDefinition> void create(C crd) {

    }

    @Override
    public <C extends CustomResourceDefinition> void update(C crd) {

    }

    @Override
    public <C extends CustomResourceDefinition> void delete(C crd) {

    }
}
