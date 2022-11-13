package run.ikaros.server.repository;


import run.ikaros.server.entity.FileEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.FileType;

import javax.annotation.Nullable;

/**
 * @author li-guohao
 */
public interface FileRepository extends BaseRepository<FileEntity, Long> {
    Page<FileEntity> findAll(Specification<FileEntity> specification, Pageable pageable);

    List<FileEntity> findAll(Specification<FileEntity> specification);

    Integer count(Specification<FileEntity> specification);

    @Query("select type from FileEntity where status = true")
    Set<String> findTypes();

    @Query("select place from FileEntity where status = true")
    Set<String> findPlaces();

    List<FileEntity> findByMd5(String md5);

    FileEntity findFileEntityByNameAndType(String name, FileType type);


    FileEntity findFileEntityByNameAndTypeAndPlace(String name, FileType type, FilePlace place);

}
