package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.MetadataService;
import run.ikaros.server.model.dto.MetadataDTO;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetadataServiceImpl implements MetadataService {
    @Nonnull
    @Override
    public List<MetadataDTO> search(@Nonnull String keyword) {
        AssertUtils.notBlank(keyword, "keyword");
        List<MetadataDTO> metadataDTOList = new ArrayList<>();
        return metadataDTOList;
    }
}
