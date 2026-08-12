package run.ikaros.server.custom;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.wrap.PagingWrap;

@Deprecated
public interface CustomClient {

    <C> C create(C custom);

    <C> C update(C custom);

    <C> void updateOneMeta(@NonNull Class<C> clazz, @NotBlank String name,
                           @NotBlank String metaName, byte @Nullable [] metaNewVal);

    <C> byte[] fetchOneMeta(@NonNull Class<C> clazz, @NotBlank String name,
                            @NotBlank String metaName);

    <C> void delete(C custom);

    <C> C delete(Class<C> clazz, String name);

    void deleteAll();

    <C> C findOne(Class<C> type, String name);

    /**
     * find all with page.
     *
     * @param type      custom class type
     * @param page      start for 1
     * @param size      size
     * @param predicate predicate
     * @param <C>       custom class type
     * @return PagingWrap
     */
    <C> PagingWrap<C> findAllWithPage(@NonNull Class<C> type,
                                      @Nullable Integer page, @Nullable Integer size,
                                      @Nullable Predicate<C> predicate);

    <C> List<C> findAll(@NonNull Class<C> type, @Nullable Predicate<C> predicate);
}
