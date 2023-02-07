package run.ikaros.server.core.subject;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CollectionEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.EpisodeFileEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.SubjectImageEntity;
import run.ikaros.server.store.repository.CollectionRepository;
import run.ikaros.server.store.repository.EpisodeFileRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectImageRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final CollectionRepository collectionRepository;
    private final SubjectImageRepository subjectImageRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeFileRepository episodeFileRepository;

    /**
     * Construct a {@link SubjectService} instance.
     *
     * @param subjectRepository      {@link SubjectEntity} repository
     * @param collectionRepository   {@link CollectionEntity} repository
     * @param subjectImageRepository {@link SubjectImageEntity} repository
     * @param episodeRepository      {@link EpisodeEntity} repository
     * @param episodeFileRepository  {@link EpisodeFileEntity} repository
     */
    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              CollectionRepository collectionRepository,
                              SubjectImageRepository subjectImageRepository,
                              EpisodeRepository episodeRepository,
                              EpisodeFileRepository episodeFileRepository) {
        this.subjectRepository = subjectRepository;
        this.collectionRepository = collectionRepository;
        this.subjectImageRepository = subjectImageRepository;
        this.episodeRepository = episodeRepository;
        this.episodeFileRepository = episodeFileRepository;
    }

    @Override
    public Mono<Subject> findById(Long subjectId) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        return Mono.just(subjectId)
            .flatMap(subjectRepository::findById)
            .map(subjectEntity -> {
                var subject = new Subject();
                BeanUtils.copyProperties(subjectEntity, subject);
                return subject;
            })
            .flatMap(subject -> subjectImageRepository.findBySubjectId(subject.getId())
                .map(subjectImageEntity -> {
                        var subjectImage = new SubjectImage();
                        BeanUtils.copyProperties(subjectImageEntity, subjectImage);
                        return subjectImage;
                    }
                )
                .flatMap(subjectImage -> Mono.just(subject)
                    .map(sub -> sub.setImage(subjectImage))))
            .flatMap(subject -> episodeRepository.findBySubjectId(subject.getId())
                .map(episodeEntity -> {
                    var episode = new Episode();
                    BeanUtils.copyProperties(episodeEntity, episode);
                    return episode;
                })
                .collectList()
                .map(episodes -> subject.setTotalEpisodes((long) episodes.size())
                    .setEpisodes(episodes)))
            // .flatMap(subject -> )
            .then(Mono.empty());
    }

    @Override
    public Mono<Subject> findByBgmId(Long bgmtvId) {
        Assert.isTrue(bgmtvId > 0, "'bgmtvId' must gt 0.");
        return Mono.just(bgmtvId)
            .flatMap(subjectRepository::findByBgmtvId)
            .flatMap(subjectEntity -> findById(subjectEntity.getId()));
    }
}
