package run.ikaros.server.crd.scheme;

import jakarta.annotation.Nonnull;
import org.springframework.lang.NonNull;
import run.ikaros.server.crd.CustomResourceDefinition;
import run.ikaros.server.crd.GroupVersionKind;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author: li-guohao
 */
public interface CRDSchemeManager {
    void register(@NonNull CRDScheme scheme);

    void unregister(@Nonnull CRDScheme scheme);

    default int size() {
        return schemes().size();
    }

    @NonNull
    List<CRDScheme> schemes();

    @NonNull
    default Optional<CRDScheme> fetch(@NonNull GroupVersionKind gvk) {
        return schemes().stream()
            .filter(scheme -> Objects.equals(scheme.groupVersionKind(), gvk))
            .findFirst();
    }

    @NonNull
    default CRDScheme get(@NonNull GroupVersionKind gvk) {
        return fetch(gvk).orElseThrow(
            () -> new CRDSchemeNotFoundException("Scheme was not found for " + gvk));
    }

    @NonNull
    default CRDScheme get(Class<? extends CustomResourceDefinition> type) {
        var gvk = CRDScheme.getGvkFromType(type);
        return get(new GroupVersionKind(gvk.group(), gvk.version(), gvk.kind()));
    }

    @NonNull
    default CRDScheme get(CustomResourceDefinition crd) {
        var gvk = crd.groupVersionKind();
        return get(gvk);
    }
}
