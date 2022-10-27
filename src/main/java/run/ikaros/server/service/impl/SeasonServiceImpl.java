package run.ikaros.server.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.repository.SeasonRepository;
import run.ikaros.server.service.SeasonService;
import run.ikaros.server.service.base.AbstractCrudService;
import run.ikaros.server.utils.AssertUtils;

/**
 * @author li-guohao
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class SeasonServiceImpl
    extends AbstractCrudService<SeasonEntity, Long>
    implements SeasonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonServiceImpl.class);

    private final SeasonRepository seasonRepository;

    public SeasonServiceImpl(SeasonRepository seasonRepository) {
        super(seasonRepository);
        this.seasonRepository = seasonRepository;
    }

    @Nonnull
    @Override
    public List<SeasonEntity> findByAnimeId(@Nonnull Long animeId) {
        AssertUtils.isPositive(animeId, "animeId");
        SeasonEntity seasonEntityExample = new SeasonEntity().setAnimeId(animeId);
        seasonEntityExample.setStatus(true);
        return listAll(Example.of(seasonEntityExample));
    }

    @Override
    public List<String> finSeasonTypes() {
        return Arrays.stream(SeasonType.values())
            .flatMap((Function<SeasonType, Stream<String>>) seasonType
                -> Stream.of(seasonType.name()))
            .toList();
    }
}
