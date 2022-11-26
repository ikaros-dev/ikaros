package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.MetadataService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.model.response.MetadataSearchResponse;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetadataServiceImpl implements MetadataService {

    private final BgmTvService bgmTvService;

    public MetadataServiceImpl(BgmTvService bgmTvService) {
        this.bgmTvService = bgmTvService;
    }

    @Nonnull
    @Override
    public List<MetadataSearchResponse> search(@Nonnull String keyword) {
        AssertUtils.notBlank(keyword, "keyword");
        List<MetadataSearchResponse> metadataSearchResponseList = new ArrayList<>();
        List<BgmTvSubject> bgmTvSubjects =
            bgmTvService.searchSubject(keyword, BgmTvSubjectType.ANIME);
        for (BgmTvSubject bgmTvSubject : bgmTvSubjects) {
            MetadataSearchResponse metadataSearchResponse = new MetadataSearchResponse();
            metadataSearchResponse.setName(bgmTvSubject.getName());
            metadataSearchResponse.setNameCn(bgmTvSubject.getNameCn());
            metadataSearchResponse.setDescription(bgmTvSubject.getSummary());
            metadataSearchResponse.setBgmTvSubjectId(bgmTvSubject.getId());
            metadataSearchResponse.setUrl(bgmTvSubject.getUrl());
            metadataSearchResponse.setImage(bgmTvSubject.getImages().getLarge());
            metadataSearchResponseList.add(metadataSearchResponse);
        }
        return metadataSearchResponseList;
    }
}
