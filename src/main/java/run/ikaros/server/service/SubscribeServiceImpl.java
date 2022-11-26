package run.ikaros.server.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.SubscribeRepository;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.SubscribeService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.SubscribeEntity;
import run.ikaros.server.enums.SubscribeType;
import run.ikaros.server.event.SubscribeBgmTvSubjectEvent;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;

@Service
public class SubscribeServiceImpl
    extends AbstractCrudService<SubscribeEntity, Long>
    implements SubscribeService, ApplicationContextAware {
    private final SubscribeRepository subscribeRepository;
    private final AnimeService animeService;
    private final BgmTvService bgmTvService;

    private ApplicationContext applicationContext;

    public SubscribeServiceImpl(
        SubscribeRepository subscribeRepository, AnimeService animeService,
        BgmTvService bgmTvService) {
        super(subscribeRepository);
        this.subscribeRepository = subscribeRepository;
        this.animeService = animeService;
        this.bgmTvService = bgmTvService;
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void subscribeBgmTvSubject(@Nonnull Long bgmTvSubjectId) {
        AssertUtils.isPositive(bgmTvSubjectId, "bgmtv subject id");

        AnimeDTO animeDTO = bgmTvService.reqBgmtvSubject(bgmTvSubjectId);
        if (animeDTO == null) {
            throw new RuntimeIkarosException("not exists for target bgmtv subject id: "
                + bgmTvSubjectId);
        }
        Long animeId = animeDTO.getId();

        save(new SubscribeEntity()
            .setType(SubscribeType.ANIME)
            .setTargetId(animeId)
            .setUserId(UserService.getCurrentLoginUserUid()));

        SubscribeBgmTvSubjectEvent subscribeBgmTvSubjectEvent =
            new SubscribeBgmTvSubjectEvent(this, bgmTvSubjectId, animeId);
        applicationContext.publishEvent(subscribeBgmTvSubjectEvent);
    }

}
