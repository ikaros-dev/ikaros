package run.ikaros.api.custom.scheme;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.lang.NonNull;
import run.ikaros.api.custom.GroupVersionKind;
import run.ikaros.api.custom.exception.CustomSchemeNotFoundException;

public interface CustomSchemeManager {

    void register(@NonNull CustomScheme scheme);

    /**
     * Registers a Custom using its type.
     *
     * @param type is Custom type.
     * @param <C>  Custom class.
     */
    default <C> void register(Class<C> type) {
        register(CustomScheme.buildFromType(type));
    }


    void unregister(@NonNull CustomScheme scheme);

    default int size() {
        return schemes().size();
    }

    @NonNull
    List<CustomScheme> schemes();

    /**
     * fetch {@link CustomScheme} by {@link GroupVersionKind}.
     */
    @NonNull
    default Optional<CustomScheme> fetch(@NonNull GroupVersionKind gvk) {
        return schemes().stream()
            .filter(scheme -> Objects.equals(scheme.groupVersionKind(), gvk))
            .findFirst();
    }

    @NonNull
    default CustomScheme get(@NonNull GroupVersionKind gvk) {
        return fetch(gvk).orElseThrow(
            () -> new CustomSchemeNotFoundException(gvk));
    }


    @NonNull
    default <C> CustomScheme get(Class<?> type) {
        return CustomScheme.buildFromType(type);
    }

}
