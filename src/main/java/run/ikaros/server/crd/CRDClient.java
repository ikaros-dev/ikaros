package run.ikaros.server.crd;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author: li-guohao
 */
public interface CRDClient {
    <C extends CustomResourceDefinition> List<C> list(Class<C> type, Predicate<C> predicate,
                                                      Comparator<C> comparator);

    <C extends CustomResourceDefinition> PageListResult<C> list(Class<C> type,
                                                                Predicate<C> predicate,
                                                                Comparator<C> comparator,
                                                                int page,
                                                                int size);

    <C extends CustomResourceDefinition> Optional<C> fetch(Class<C> type, String name);

    Optional<CustomResourceDefinition> fetch(GroupVersionKind gvk, String name);

    <C extends CustomResourceDefinition> void create(C crd);

    <C extends CustomResourceDefinition> void update(C crd);

    <C extends CustomResourceDefinition> void delete(C crd);

}
