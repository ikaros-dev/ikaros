package cn.liguohao.ikaros.repository;


import cn.liguohao.ikaros.model.entity.FileEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author li-guohao
 */
public interface FileRepository extends BaseRepository<FileEntity> {
    Page<FileEntity> findAll(Specification<FileEntity> specification, Pageable pageable);

    List<FileEntity> findAll(Specification<FileEntity> specification);

    Integer count(Specification<FileEntity> specification);
}
