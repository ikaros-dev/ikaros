package run.ikaros.server.service;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import run.ikaros.server.core.service.CrudService;
import run.ikaros.server.entity.BaseEntity;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.core.repository.BaseRepository;
import run.ikaros.server.utils.AssertUtils;

/**
 * @param <E> entity
 * @param <I> id
 * @author li-guohao
 */
public class AbstractCrudService<E, I> implements CrudService<E, I> {

    private final BaseRepository<E, I> baseRepository;

    public AbstractCrudService(BaseRepository<E, I> baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public void flush() {
        baseRepository.flush();
    }

    @Override
    public long count() {
        return baseRepository.count();
    }

    @Override
    public boolean existsById(@Nonnull I id) {
        return baseRepository.existsById(id);
    }

    @Nonnull
    @Override
    public E save(@Nonnull E entity) {
        AssertUtils.notNull(entity, "entity");
        return baseRepository.saveAndFlush(entity);
    }

    @Nonnull
    @Override
    public E save(@Nonnull E entity, boolean flush) {
        AssertUtils.notNull(entity, "entity");
        return flush ? baseRepository.saveAndFlush(entity) : baseRepository.save(entity);
    }

    @Override
    public E deleteById(@Nonnull I id) {
        AssertUtils.notNull(id, "id");
        E e = getById(id);
        baseRepository.delete(e);
        return e;
    }

    @Override
    public E removeById(@Nonnull I id) {
        E e = getById(id);
        BaseEntity entity = (BaseEntity) e;
        entity.setStatus(false);
        e = (E) entity;
        return baseRepository.saveAndFlush(e);
    }

    @Nonnull
    @Override
    public List<E> listAll() {
        return baseRepository.findAll();
    }

    @Nonnull
    @Override
    public List<E> listAll(@Nonnull Sort sort) {
        AssertUtils.notNull(sort, "sort");
        return baseRepository.findAll(sort);
    }

    @Nonnull
    @Override
    public Page<E> listAll(@Nonnull Pageable pageable) {
        AssertUtils.notNull(pageable, "pageable");
        return baseRepository.findAll(pageable);
    }

    @Nonnull
    @Override
    public List<E> listAll(@Nonnull Example<E> example) {
        AssertUtils.notNull(example, "example");
        return baseRepository.findAll(example);
    }

    @Nonnull
    @Override
    public Page<E> listAll(@Nonnull Example<E> example, @Nonnull Pageable pageable) {
        AssertUtils.notNull(example, "example");
        AssertUtils.notNull(pageable, "pageable");
        return baseRepository.findAll(example, pageable);
    }

    @Nonnull
    @Override
    public Optional<E> fetchById(@Nonnull I id) {
        AssertUtils.notNull(id, "id");
        return baseRepository.findById(id);
    }

    @Nonnull
    @Override
    public E getById(@Nonnull I id) {
        AssertUtils.notNull(id, "id");
        return baseRepository.findById(id).orElseThrow(
            () -> new RecordNotFoundException("record not found, id=" + id));
    }

    @Override
    public void deleteAll() {
        baseRepository.deleteAll();
    }
}
