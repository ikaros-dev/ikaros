package run.ikaros.server.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author li-guohao
 */
@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, Long> {
    boolean existsByIdAndStatus(Long id, Boolean status);

    Optional<T> findByIdAndStatus(Long id, boolean status);

}
