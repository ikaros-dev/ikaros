package run.ikaros.server.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import run.ikaros.server.constants.TemplateConst;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.NotifyService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.entity.UserEntity;
import run.ikaros.server.event.EpisodeUrlUpdateEvent;
import run.ikaros.server.utils.StringUtils;

import java.util.HashMap;

@Slf4j
@Component
public class EpisodeUrlUpdateEventListener
    implements ApplicationListener<EpisodeUrlUpdateEvent> {

    private final NotifyService notifyService;
    private final SeasonService seasonService;
    private final AnimeService animeService;
    private final UserService userService;

    public EpisodeUrlUpdateEventListener(NotifyService notifyService, SeasonService seasonService,
                                         AnimeService animeService, UserService userService) {
        this.notifyService = notifyService;
        this.seasonService = seasonService;
        this.animeService = animeService;
        this.userService = userService;
    }

    @Override
    public void onApplicationEvent(@NonNull EpisodeUrlUpdateEvent event) {
        EpisodeEntity episodeEntity = event.getEpisodeEntity();
        final String oldUrl = event.getOldUrl();
        if (episodeEntity != null) {
            Long seasonId = episodeEntity.getSeasonId();
            SeasonEntity seasonEntity = seasonService.getById(seasonId);
            if (seasonEntity == null) {
                log.warn("not found season for episode id={}, title={}", episodeEntity.getId(),
                    episodeEntity.getTitle());
                return;
            }
            AnimeEntity animeEntity = animeService.getById(seasonEntity.getAnimeId());
            if (animeEntity == null) {
                log.warn("not found anime for episode id={}, title={}", episodeEntity.getId(),
                    episodeEntity.getTitle());
                return;
            }

            if (StringUtils.isNotBlank(oldUrl) && oldUrl.equalsIgnoreCase(episodeEntity.getUrl())) {
                log.warn("new url equals old url, skip notify, url:{}", oldUrl);
                return;
            }

            if (StringUtils.isBlank(episodeEntity.getUrl())) {
                log.warn("new url is blank, skip notify");
                return;
            }

            final String title = StringUtils.isNotBlank(animeEntity.getTitleCn())
                ? animeEntity.getTitleCn() : animeEntity.getTitle();
            final String subject = "番剧更新: 《" + title + "》";
            UserEntity userOnlyOne = userService.getUserOnlyOne();
            if (userOnlyOne == null) {
                log.warn("app not init user");
                return;
            }
            String targetAddress = userOnlyOne.getEmail();
            if (StringUtils.isBlank(targetAddress)) {
                log.error("user not set email, skip notify for episode id={}, title={}",
                    episodeEntity.getId(), episodeEntity.getTitle());
                return;
            }

            final var context = new Context();
            var vars = new HashMap<String, Object>();
            vars.put("title", title);
            vars.put("epTitle",
                StringUtils.isNotBlank(episodeEntity.getTitleCn())
                    ? episodeEntity.getTitleCn() : episodeEntity.getTitle());
            vars.put("epSeq", episodeEntity.getSeq());
            vars.put("epIntroduction", episodeEntity.getOverview());
            vars.put("introduction", animeEntity.getOverview());
            context.setVariables(vars);

            notifyService.sendTemplateMail(targetAddress, subject,
                TemplateConst.MAIL_ANIME_UPDATE, context);
        }
    }
}
