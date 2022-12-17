package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.MetadataService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.model.response.MetadataSearchResponse;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;
import run.ikaros.server.utils.AssertUtils;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MetadataServiceImpl implements MetadataService {

    private final BgmTvService bgmTvService;
    private final AnimeService animeService;

    public MetadataServiceImpl(BgmTvService bgmTvService, AnimeService animeService) {
        this.bgmTvService = bgmTvService;
        this.animeService = animeService;
    }

    @Nonnull
    @Override
    public List<MetadataSearchResponse> search(@Nonnull String keyword) {
        AssertUtils.notBlank(keyword, "keyword");
        List<MetadataSearchResponse> metadataSearchResponseList = new ArrayList<>();

        // 这里先根据关键词查一遍数据库
        List<AnimeEntity> animeEntityList = animeService.findByTitleLike(keyword);
        if (animeEntityList.isEmpty()) {
            animeEntityList = animeService.findByTitleCnLike(keyword);
        }

        if (animeEntityList.isEmpty()) {
            // 实在查不到再请求番组计划
            List<BgmTvSubject> bgmTvSubjects =
                bgmTvService.searchSubject(keyword, BgmTvSubjectType.ANIME);
            for (BgmTvSubject bgmTvSubject : bgmTvSubjects) {
                bgmTvService.reqBgmtvSubject(Long.valueOf(bgmTvSubject.getId()));
            }
        }

        // 最后再查数据库
        animeEntityList = animeService.findByTitleLike(keyword);
        if (animeEntityList.isEmpty()) {
            animeEntityList = animeService.findByTitleCnLike(keyword);
        }

        if (!animeEntityList.isEmpty()) {
            for (AnimeEntity animeEntity : animeEntityList) {
                MetadataSearchResponse metadataSearchResponse = new MetadataSearchResponse();
                metadataSearchResponse.setName(animeEntity.getTitle());
                metadataSearchResponse.setNameCn(animeEntity.getTitleCn());
                metadataSearchResponse.setDescription(animeEntity.getOverview());
                metadataSearchResponse.setBgmTvSubjectId(Math.toIntExact(animeEntity.getBgmtvId()));
                metadataSearchResponse.setImage(animeEntity.getCoverUrl());
                metadataSearchResponseList.add(metadataSearchResponse);
            }
        }
        return metadataSearchResponseList;
    }
}
