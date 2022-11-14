package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.entity.MikanEpUrlBgmTvSubjectIdEntity;
import run.ikaros.server.core.repository.MikanEpUrlBgmTvSubjectIdRepository;
import run.ikaros.server.core.service.MikanEpUrlBgmTvSubjectIdService;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Service
public class MikanEpUrlBgmTvSubjectIdImpl
    extends AbstractCrudService<MikanEpUrlBgmTvSubjectIdEntity, Long>
    implements MikanEpUrlBgmTvSubjectIdService {

    private final MikanEpUrlBgmTvSubjectIdRepository repository;

    public MikanEpUrlBgmTvSubjectIdImpl(MikanEpUrlBgmTvSubjectIdRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public boolean existsByMikanEpisodeUrl(@Nonnull String mikanEpisodeUrl) {
        AssertUtils.notBlank(mikanEpisodeUrl, "mikanEpisodeUrl");
        return repository.existsByMikanEpisodeUrl(mikanEpisodeUrl);
    }

    @Nullable
    @Override
    public MikanEpUrlBgmTvSubjectIdEntity findByMikanEpisodeUrl(@Nonnull String mikanEpisodeUrl) {
        AssertUtils.notBlank(mikanEpisodeUrl, "mikanEpisodeUrl");
        return repository.findByMikanEpisodeUrl(mikanEpisodeUrl);
    }
}
