package run.ikaros.server.core.service;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author li-guohao
 * @param <E> Entity
 */
public interface CrudService<E, I> {

    void flush();

    long count();

    boolean existsById(@Nonnull I id);

    @Nonnull
    @Transactional
    E save(@Nonnull E entity);

    @Nonnull
    @Transactional
    E save(@Nonnull E entity, boolean flush);

    @Nullable
    @Transactional
    E removeById(@Nonnull I id);

    @Nonnull
    List<E> listAll();

    @Nonnull
    List<E> listAll(@Nonnull Sort sort);

    @Nonnull
    Page<E> listAll(@Nonnull Pageable pageable);

    @Nonnull
    List<E> listAll(@Nonnull Example<E> example);

    @Nonnull
    Page<E> listAll(@Nonnull Example<E> example, @Nonnull Pageable pageable);

    @Nonnull
    Optional<E> fetchById(@Nonnull I id);

    @Nonnull
    E getById(@Nonnull I id);

    void removeAll();
}
