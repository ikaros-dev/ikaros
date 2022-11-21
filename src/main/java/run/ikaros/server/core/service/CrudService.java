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
import run.ikaros.server.entity.BaseEntity;

/**
 * @param <E> Entity
 * @author li-guohao
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

    /**
     * 真实删除，根据ID，同时会移除对应文件系统上的资源
     *
     * @param id entity ID
     * @return entity
     */
    @Nullable
    @Transactional
    E deleteById(@Nonnull I id);

    /**
     * 逻辑删除，根据ID，只会将状态更新为 false, 不会移除表记录和文件系统上的资源
     *
     * @param id entity ID
     * @return entity
     */
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

    /**
     * 真实删除所有数据
     */
    void deleteAll();
}
