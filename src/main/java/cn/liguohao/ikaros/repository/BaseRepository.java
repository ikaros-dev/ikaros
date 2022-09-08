package cn.liguohao.ikaros.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author li-guohao
 */
@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, Long> {

    Optional<T> findByIdAndStatus(Long id, boolean status);

}
