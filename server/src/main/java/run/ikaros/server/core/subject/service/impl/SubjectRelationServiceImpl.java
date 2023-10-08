package run.ikaros.server.core.subject.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.core.subject.SubjectRelationCourt;
import run.ikaros.server.core.subject.service.SubjectRelationService;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.SubjectRelationEntity;
import run.ikaros.server.store.repository.SubjectRelationRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@Service
public class SubjectRelationServiceImpl implements SubjectRelationService {
    private final SubjectRelationRepository subjectRelationRepository;
    private final SubjectRepository subjectRepository;

    public SubjectRelationServiceImpl(SubjectRelationRepository subjectRelationRepository,
                                      SubjectRepository subjectRepository) {
        this.subjectRelationRepository = subjectRelationRepository;
        this.subjectRepository = subjectRepository;
    }


    @Override
    public Flux<SubjectRelation> findAllBySubjectId(Long subjectId) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt zero.");
        return subjectRelationRepository.findAllBySubjectId(subjectId)
            .collectList()
            .flatMapMany(subjectRelationEntities -> {
                Map<SubjectRelationType, SubjectRelation> typeSubjectRelationMap = new HashMap<>();
                subjectRelationEntities.forEach(subjectRelationEntity -> {
                    SubjectRelationType relationType = subjectRelationEntity.getRelationType();
                    if (typeSubjectRelationMap.containsKey(relationType)) {
                        SubjectRelation subjectRelation = typeSubjectRelationMap.get(relationType);
                        subjectRelation.getRelationSubjects()
                            .add(subjectRelationEntity.getRelationSubjectId());
                    } else {
                        var relationSubjectSet = new HashSet<Long>();
                        relationSubjectSet.add(subjectRelationEntity.getRelationSubjectId());
                        SubjectRelation subjectRelation = SubjectRelation.builder()
                            .subject(subjectId)
                            .relationType(relationType)
                            .relationSubjects(relationSubjectSet)
                            .build();
                        typeSubjectRelationMap.put(relationType, subjectRelation);
                    }
                });
                return Flux.fromStream(typeSubjectRelationMap.values().stream());
            });
    }

    @Override
    public Mono<SubjectRelation> findBySubjectIdAndType(Long subjectId,
                                                        SubjectRelationType relationType) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt zero.");
        Assert.notNull(relationType, "'relationType' must not be null.");
        return subjectRelationRepository
            .findAllBySubjectIdAndRelationType(subjectId, relationType)
            .map(SubjectRelationEntity::getRelationSubjectId)
            .collect(Collectors.toSet())
            .flatMap(relationSubjects -> Mono.just(SubjectRelation.builder()
                .relationSubjects(relationSubjects)
                .subject(subjectId)
                .relationType(relationType)
                .build()));
    }

    @Override
    public Mono<SubjectRelation> createSubjectRelation(SubjectRelation subjectRelation) {
        Assert.notNull(subjectRelation, "'subjectRelation' must not be null.");
        Assert.isTrue(subjectRelation.getSubject() > 0, "'subjectRelation' must not be null.");
        return findBySubjectIdAndType(subjectRelation.getSubject(),
            subjectRelation.getRelationType())
            .map(SubjectRelation::getRelationSubjects)
            .switchIfEmpty(Mono.just(new HashSet<>()))
            .flatMapMany(
                existsRelationSubjectSet -> Flux.fromStream(subjectRelation.getRelationSubjects()
                        .stream())
                    .filter(relationSubject -> !existsRelationSubjectSet.contains(relationSubject))
                    // save master relation
                    .map(relationSubject -> SubjectRelationEntity.builder()
                        .subjectId(subjectRelation.getSubject())
                        .relationType(subjectRelation.getRelationType())
                        .relationSubjectId(relationSubject)
                        .build())
                    .flatMap(subjectRelationRepository::save)
                    // save slave relation
                    .flatMap(subjectRelationEntity -> subjectRepository.findById(
                            subjectRelationEntity.getSubjectId())
                        .map(SubjectEntity::getType)
                        .flatMap(subjectType -> Mono.just(SubjectRelationEntity.builder()
                            .subjectId(subjectRelationEntity.getRelationSubjectId())
                            .relationSubjectId(subjectRelationEntity.getSubjectId())
                            .relationType(SubjectRelationCourt.judge(subjectType,
                                subjectRelationEntity.getRelationType()))
                            .build())))
                    .flatMap(subjectRelationRepository::save)

            )
            .then(findBySubjectIdAndType(subjectRelation.getSubject(),
                subjectRelation.getRelationType()));
    }

    @Override
    public Mono<SubjectRelation> removeSubjectRelation(SubjectRelation subjectRelation) {
        Assert.notNull(subjectRelation, "'subjectRelation' must not be null.");
        Assert.isTrue(subjectRelation.getSubject() > 0, "'subjectRelation' must not be null.");
        return findBySubjectIdAndType(subjectRelation.getSubject(),
            subjectRelation.getRelationType())
            .map(SubjectRelation::getRelationSubjects)
            .switchIfEmpty(Mono.just(new HashSet<>()))
            .flatMapMany(existsRelationSubjectSet
                -> Flux.fromStream(subjectRelation.getRelationSubjects().stream())
                .filter(existsRelationSubjectSet::contains)
                .flatMap(relationSubject
                    -> subjectRelationRepository
                    // delete master relation
                    .deleteBySubjectIdAndRelationTypeAndRelationSubjectId(
                        subjectRelation.getSubject(),
                        subjectRelation.getRelationType(), relationSubject)
                    // delete slave relation
                    .then(subjectRepository.findById(subjectRelation.getSubject())
                        .map(SubjectEntity::getType)
                        .map(subjectType -> SubjectRelationCourt.judge(subjectType,
                            subjectRelation.getRelationType())))
                    .flatMap(relationType -> subjectRelationRepository
                        .deleteBySubjectIdAndRelationTypeAndRelationSubjectId(relationSubject,
                            relationType, subjectRelation.getSubject()))
                )

            )
            .then(Mono.just(subjectRelation));
    }
}
