package run.ikaros.server.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.SongRepository;
import run.ikaros.server.core.service.SongService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.entity.BaseEntity;
import run.ikaros.server.entity.SongEntity;
import run.ikaros.server.model.dto.SongDTO;
import run.ikaros.server.model.request.SearchSongRequest;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class SongServiceImpl extends AbstractCrudService<SongEntity, Long> implements SongService {
    private final SongRepository songRepository;
    private final UserService userService;

    public SongServiceImpl(SongRepository songRepository, UserService userService) {
        super(songRepository);
        this.songRepository = songRepository;
        this.userService = userService;
    }


    @Nonnull
    @Override
    public PagingWrap<SongDTO> findSongs(SearchSongRequest searchSongRequest) {
        List<SongEntity> songEntities = new ArrayList<>();
        if (null == searchSongRequest) {
            songEntities.addAll(listAll()
                .stream()
                .filter(BaseEntity::getStatus)
                .toList());
        } else {
            Integer pageIndex = searchSongRequest.getPage();
            Integer pageSize = searchSongRequest.getSize();
            String keyword = searchSongRequest.getKeyword();

            if (pageIndex == null) {
                pageIndex = 1;
            }

            if (pageSize == null) {
                pageSize = 20;
            }

            // 构造自定义查询条件
            Specification<SongEntity> queryCondition = (root, criteriaQuery, criteriaBuilder) -> {
                List<Predicate> predicateList = new ArrayList<>();

                // 过滤掉逻辑删除的
                predicateList.add(criteriaBuilder.equal(root.get("status"), true));

                if (StringUtils.isNotBlank(keyword)) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + keyword + "%"));
                }

                Predicate[] predicates = new Predicate[predicateList.size()];
                return criteriaBuilder.and(predicateList.toArray(predicates));
            };

            // page小于1时，都为第一页, page从1开始，即第一页 pageIndex=1
            songEntities.addAll(
                songRepository.findAll(queryCondition,
                        PageRequest.of(pageIndex < 1 ? 0 : (pageIndex - 1), pageSize,
                            Sort.by(Sort.Direction.DESC, "createTime")))
                    .getContent()
            );
        }

        PagingWrap<SongDTO> songDTOPagingWrap = new PagingWrap<>();
        List<SongDTO> songDTOList = new ArrayList<>();
        for (SongEntity songEntity : songEntities) {
            SongDTO songDTO = new SongDTO();
            songDTO.setName(songEntity.getName());
            songDTO.setMetadata(JsonUtils.json2obj(songEntity.getMetadata(), HashMap.class));
            songDTO.setAlbumId(songEntity.getAlbumId());
            songDTO.setMenuId(songEntity.getMenuId());
            songDTO.setUploadTime(songEntity.getCreateTime());
            songDTO.setUploadUser(userService.getById(songEntity.getCreateUid()).getUsername());
            songDTO.setUrl(songEntity.getUrl());
            songDTOList.add(songDTO);
        }

        songDTOPagingWrap.setContent(songDTOList);
        return songDTOPagingWrap;
    }
}
