package run.ikaros.server.store.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import run.ikaros.server.store.entity.BaseEntity;

/**
 * all entity repository base, private key is Long id.
 *
 * @param <E> entity need extends BaseEntity
 * @author li-guohao
 * @see BaseEntity
 */
@NoRepositoryBean
public interface BaseRepository<E extends BaseEntity> extends JpaRepository<E, Long> {
    /**
     * find record exists by id and status.
     *
     * @param id     record private key
     * @param status record status
     * @return true when exists
     */
    Boolean existsByIdAndStatus(Long id, Boolean status);

    /**
     * find record optional by id and status.
     *
     * @param id     record private key
     * @param status record status
     * @return record optional
     */
    Optional<E> findByIdAndStatus(Long id, boolean status);

}
