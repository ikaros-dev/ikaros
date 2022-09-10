package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.BeanKit;
import cn.liguohao.ikaros.entity.anime.AnimeEntity;
import cn.liguohao.ikaros.entity.anime.AnimeSeasonEntity;
import cn.liguohao.ikaros.entity.anime.EpisodeEntity;
import cn.liguohao.ikaros.entity.anime.SeasonEntity;
import cn.liguohao.ikaros.entity.anime.SeasonEpisodeEntity;
import cn.liguohao.ikaros.model.Anime;
import cn.liguohao.ikaros.model.Episode;
import cn.liguohao.ikaros.model.Season;
import cn.liguohao.ikaros.repository.anime.AnimeRepository;
import cn.liguohao.ikaros.repository.anime.AnimeSeasonRepository;
import cn.liguohao.ikaros.repository.anime.EpisodeRepository;
import cn.liguohao.ikaros.repository.anime.SeasonEpisodeRepository;
import cn.liguohao.ikaros.repository.anime.SeasonRepository;
import java.util.Date;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class AnimeService {

    private final AnimeRepository animeRepository;
    private final AnimeSeasonRepository animeSeasonRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonEpisodeRepository seasonEpisodeRepository;
    private final EpisodeRepository episodeRepository;

    private final UserService userService;

    public AnimeService(AnimeRepository animeRepository,
                        AnimeSeasonRepository animeSeasonRepository,
                        SeasonRepository seasonRepository,
                        SeasonEpisodeRepository seasonEpisodeRepository,
                        EpisodeRepository episodeRepository, UserService userService) {
        this.animeRepository = animeRepository;
        this.animeSeasonRepository = animeSeasonRepository;
        this.seasonRepository = seasonRepository;
        this.seasonEpisodeRepository = seasonEpisodeRepository;
        this.episodeRepository = episodeRepository;
        this.userService = userService;
    }


    public Anime addAnime(Anime anime) {
        Assert.notNull(anime, "'anime' must not be null.");

        final Date now = new Date();
        final Long loginUserUid = userService.getCurrentLoginUserUid();

        Assert.notNull(anime.getTitle(), "'anime#getTitle' must not be null.");
        AnimeEntity animeEntity = new AnimeEntity();
        animeEntity.setTimeAndUidWhenCreate(now, loginUserUid);
        BeanKit.copyProperties(anime, animeEntity);
        animeEntity = animeRepository.saveAndFlush(animeEntity);

        Long animeId = animeEntity.getId();
        Assert.notNull(animeId, "'animeId' must not be null");
        anime.setId(animeId);

        List<Season> seasons = anime.getSeasons();
        Assert.notNull(seasons, "'seasons' must not be null");
        for (Season season : seasons) {
            SeasonEntity seasonEntity = new SeasonEntity();
            seasonEntity.setTimeAndUidWhenCreate(now, loginUserUid);
            BeanKit.copyProperties(season, seasonEntity);
            seasonEntity = seasonRepository.saveAndFlush(seasonEntity);


            Long seasonId = seasonEntity.getId();
            Assert.notNull(seasonId, "'seasonId' must not be null");
            season.setId(seasonId);

            AnimeSeasonEntity animeSeasonEntity = new AnimeSeasonEntity();
            animeSeasonEntity.setTimeAndUidWhenCreate(now, loginUserUid);
            animeSeasonEntity.setAnimeId(animeId)
                .setSeasonId(seasonId);
            animeSeasonRepository.saveAndFlush(animeSeasonEntity);


            List<Episode> episodes = season.getEpisodes();
            Assert.notNull(episodes, "'episodes' must not be null");
            for (Episode episode : episodes) {
                EpisodeEntity episodeEntity = new EpisodeEntity();
                episodeEntity.setTimeAndUidWhenCreate(now, loginUserUid);
                BeanKit.copyProperties(episode, episodeEntity);
                episodeEntity = episodeRepository.saveAndFlush(episodeEntity);

                // PS: 文件上传是在保存元数据之前的，这里应该是获取到文件ID的
                Long episodeId = episodeEntity.getId();
                Assert.notNull(episodeId, "'episodeId' must not be null");
                episode.setId(episodeId);

                SeasonEpisodeEntity seasonEpisodeEntity = new SeasonEpisodeEntity();
                seasonEpisodeEntity.setTimeAndUidWhenCreate(now, loginUserUid);
                seasonEpisodeEntity.setEpisodeId(episodeId)
                    .setSeasonId(seasonId);
                seasonEpisodeRepository.saveAndFlush(seasonEpisodeEntity);
            }

        }

        return anime;
    }

}
