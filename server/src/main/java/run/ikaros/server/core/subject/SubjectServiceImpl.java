package run.ikaros.server.core.subject;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.store.entity.BaseEntity;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
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
        return subjectRepository.findById(id)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found subject record by id: " + id)))
            .flatMap(subjectEntity -> copyProperties(subjectEntity, new Subject())
                .map(subject -> subject.setType(SubjectType.codeOf(subjectEntity.getType()))))
            .flatMap(subject -> subjectImageRepository.findBySubjectId(subject.getId())
                .flatMap(
                    subjectImageEntity -> copyProperties(subjectImageEntity, new SubjectImage()))
                .flatMap(subjectImage -> Mono.just(subject.setImage(subjectImage)))
                .switchIfEmpty(Mono.just(subject)))
            .flatMap(subject -> episodeRepository.findBySubjectId(subject.getId())
                .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()))
                .collectList()
                .flatMap(episodes -> Mono.just(subject.setTotalEpisodes((long) episodes.size())
                    .setEpisodes(episodes)))
                .switchIfEmpty(Mono.just(subject)));
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
            .filter(subject1 -> Objects.nonNull(subject1.getType()))
            .switchIfEmpty(
                Mono.error(new IllegalArgumentException("subject type must not be null")))
            // Save subject entity
            .flatMap(subject1 -> copyProperties(subject1, new SubjectEntity())
                .map(subjectEntity -> subjectEntity.setType(subject1.getType().getCode()))
                .flatMap(subjectRepository::save)
                .flatMap(subjectEntity -> Mono.just(subject1.setId(subjectEntity.getId()))
                    .map(subject2 ->
                        subject2.setType(SubjectType.codeOf(subjectEntity.getType())))))
            // Save subject image entity
            .flatMap(subject1 -> Mono.just(subject1.getImage())
                .flatMap(subjectImage -> copyProperties(subjectImage, new SubjectImageEntity())
                    .map(subjectImageEntity -> subjectImageEntity.setSubjectId(subject1.getId()))
                    .flatMap(subjectImageRepository::save)
                    .map(subjectImageEntity -> subjectImage.setId(subjectImageEntity.getId()))
                    .flatMap(subjectImage1 -> Mono.just(subject1.setImage(subjectImage1)))))
            // Save episode entity
            .flatMap(subject1 -> Mono.just(subject1.getEpisodes())
                .flatMapMany(episodes -> Flux.fromStream(episodes.stream()))
                .flatMap(episode -> copyProperties(episode, new EpisodeEntity())
                    .map(episodeEntity -> episodeEntity.setSubjectId(subject1.getId()))
                    .flatMap(episodeRepository::save)
                    .flatMap(episodeEntity -> Mono.just(episode.setId(episodeEntity.getId()))))
                .collectList()
                .flatMap(episodes -> Mono.just(subject1.setEpisodes(episodes))));
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
            .then(episodeRepository.deleteAllBySubjectId(id));
    }

    @Override
    public Mono<PagingWrap<Subject>> findAllByPageable(PagingWrap<Subject> pagingWrap) {
        Assert.notNull(pagingWrap, "'pagingWrap' must not be null");
        Assert.isTrue(pagingWrap.getPage() > 0, "'pagingWrap' page must gt 0");
        Assert.isTrue(pagingWrap.getSize() > 0, "'pagingWrap' size must gt 0");
        return Mono.just(pagingWrap)
            .flatMap(pagingWrap1 ->
                subjectRepository.findAllBy(
                        PageRequest.of(pagingWrap1.getPage() - 1, pagingWrap1.getSize()))
                    .map(BaseEntity::getId)
                    .flatMap(this::findById)
                    .collectList()
                    .flatMap(subjects -> subjectRepository.count()
                        .flatMap(total -> Mono.just(
                            new PagingWrap<>(pagingWrap1.getPage(), pagingWrap1.getSize(), total,
                                subjects)))));
    }
}
