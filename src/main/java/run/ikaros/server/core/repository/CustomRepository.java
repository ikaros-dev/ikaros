package run.ikaros.server.core.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;
import run.ikaros.server.entity.CustomEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomRepository extends JpaRepository<CustomEntity, String> {
    List<CustomEntity> findAllByNameStartingWith(String prefix);
}
