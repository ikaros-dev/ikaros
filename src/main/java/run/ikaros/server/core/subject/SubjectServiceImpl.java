package run.ikaros.server.core.subject;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.exception.NotFoundException;
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
    public Mono<Subject> findById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.existsById(id)
            .filter(flag -> flag)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found subject record by id: " + id)))
            .flatMap(flag -> Mono.just(id))
            .flatMap(subjectRepository::findById)
            .flatMap(subjectEntity -> copyProperties(subjectEntity, new Subject()))
            .flatMap(subject -> subjectImageRepository.findBySubjectId(subject.getId())
                .flatMap(subjectImageEntity ->
                    copyProperties(subjectImageEntity, new SubjectImage()))
                .flatMap(subjectImage -> Mono.just(subject.setImage(subjectImage))))
            .flatMap(subject -> episodeRepository.findBySubjectId(subject.getId())
                .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()))
                .collectList()
                .map(episodes -> subject.setTotalEpisodes((long) episodes.size())
                    .setEpisodes(episodes)));
    }

    @Override
    public Mono<Subject> findByBgmId(Long bgmtvId) {
        Assert.isTrue(bgmtvId > 0, "'bgmtvId' must gt 0.");
        return Mono.just(bgmtvId)
            .flatMap(subjectRepository::findByBgmtvId)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found subject by bgmtv_id: " + bgmtvId)))
            .flatMap(subjectEntity -> findById(subjectEntity.getId()));
    }

    @Override
    public Mono<Subject> save(Subject subject) {
        Assert.notNull(subject, "'subject' must not be null.");
        return Mono.just(subject)
            // Save subject entity
            .flatMap(sub -> copyProperties(sub, new SubjectEntity()))
            .map(subjectEntity -> subjectEntity.setType(subject.getType().getCode()))
            .flatMap(subjectRepository::save)
            // Save subject image entity
            .flatMap(subEntity -> Mono.just(subject.getImage()))
            .flatMap(subjectImage -> copyProperties(subjectImage,
                new SubjectImageEntity().setSubjectId(subject.getId())))
            .flatMap(subjectImageRepository::save)
            // Save episode entity
            .flatMap(subjectImageEntity -> Mono.just(subject.getEpisodes()))
            .flatMapMany(episodes -> Flux.fromStream(episodes.stream()))
            .flatMap(episode -> copyProperties(episode,
                new EpisodeEntity().setSubjectId(subject.getId())))
            .flatMap(episodeRepository::save)
            .then(Mono.just(subject));
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.existsById(id)
            .filter(flag -> flag)
            // Delete subject entity
            .flatMap(subject -> subjectRepository.deleteById(id))
            // Delete subject image entity
            .then(subjectImageRepository.deleteBySubjectId(id))
            // Delete episode entities
            .then(episodeRepository.deleteAllBySubjectId(id))
            .then();
    }
}
