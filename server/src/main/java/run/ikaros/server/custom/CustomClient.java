package run.ikaros.server.custom;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.function.Predicate;
import run.ikaros.api.infra.model.PagingWrap;

@Deprecated
public interface CustomClient {

    <C> C create(C custom);

    <C> C update(C custom);

    <C> void updateOneMeta(@Nonnull Class<C> clazz, @NotBlank String name,
                           @NotBlank String metaName, @Nullable byte[] metaNewVal);

    <C> byte[] fetchOneMeta(@Nonnull Class<C> clazz, @NotBlank String name,
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
    <C> PagingWrap<C> findAllWithPage(@Nonnull Class<C> type,
                                      @Nullable Integer page, @Nullable Integer size,
                                      @Nullable Predicate<C> predicate);

    <C> List<C> findAll(@Nonnull Class<C> type, @Nullable Predicate<C> predicate);
}
