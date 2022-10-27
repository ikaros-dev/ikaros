package run.ikaros.server.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author li-guohao
 * @param <E> entity
 * @param <I> id
 */
@NoRepositoryBean
public interface BaseRepository<E, I> extends JpaRepository<E, I> {
    boolean existsByIdAndStatus(I id, Boolean status);

    Optional<E> findByIdAndStatus(I id, boolean status);

}
