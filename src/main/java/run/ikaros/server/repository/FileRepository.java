package run.ikaros.server.repository;


import run.ikaros.server.entity.FileEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;

/**
 * @author li-guohao
 */
public interface FileRepository extends BaseRepository<FileEntity> {
    Page<FileEntity> findAll(Specification<FileEntity> specification, Pageable pageable);

    List<FileEntity> findAll(Specification<FileEntity> specification);

    Integer count(Specification<FileEntity> specification);

    @Query("select type from FileEntity where status = true")
    Set<String> findTypes();

    @Query("select place from FileEntity where status = true")
    Set<String> findPlaces();

    List<FileEntity> findByMd5(String md5);
}
