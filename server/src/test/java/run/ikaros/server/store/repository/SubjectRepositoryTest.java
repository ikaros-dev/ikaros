package run.ikaros.server.store.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.relational.core.query.Criteria.where;

import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.entity.SubjectEntity;

@SpringBootTest
class SubjectRepositoryTest {

    @Autowired
    SubjectRepository subjectRepository;

    @Autowired
    R2dbcEntityTemplate template;

    @AfterEach
    void tearDown() {
        StepVerifier.create(subjectRepository.deleteAll()).verifyComplete();
    }

    @Test
    void findById() {
        final String name = "test" + new Random(100).nextInt();
        SubjectEntity subject = SubjectEntity.builder()
            .name(name)
            .type(SubjectType.ANIME)
            .nsfw(false)
            .build();
        StepVerifier.create(subjectRepository.save(subject))
            .expectNextMatches(subjectEntity -> {
                subject.setId(subjectEntity.getId());
                return name.equals(subjectEntity.getName());
            })
            .verifyComplete();


        StepVerifier.create(subjectRepository.findById(subject.getId()))
            .expectNextMatches(subjectEntity -> name.equalsIgnoreCase(subjectEntity.getName()))
            .verifyComplete();
    }

    @Test
    void update() {
        final String name = "test" + new Random(100).nextInt();
        SubjectEntity subjectEntity = SubjectEntity.builder()
            .name(name)
            .type(SubjectType.ANIME)
            .nsfw(false)
            .build();

        StepVerifier.create(subjectRepository.save(subjectEntity))
            .expectNext(subjectEntity).verifyComplete();

        assertThat(subjectEntity.getId()).isGreaterThan(0);

        String newName = name + new Random(10).nextInt();
        subjectEntity.setName(newName);

        String finalNewName1 = newName;
        StepVerifier.create(subjectRepository.save(subjectEntity))
            .expectNextMatches(newSubject -> finalNewName1.equals(newSubject.getName()))
            .verifyComplete();

        subjectEntity.setNsfw(true);

        StepVerifier.create(subjectRepository.save(subjectEntity))
            .expectNextMatches(SubjectEntity::getNsfw)
            .verifyComplete();

        Subject tmpSub = new Subject();
        BeanUtils.copyProperties(subjectEntity, tmpSub);
        subjectEntity = new SubjectEntity();
        BeanUtils.copyProperties(tmpSub, subjectEntity);

        assertThat(subjectEntity.getId()).isGreaterThan(0);

        newName = name + new Random(10).nextInt();
        subjectEntity.setName(newName);
        String nameCn = "测试" + new Random(100).nextInt();
        subjectEntity.setNameCn(nameCn);

        // String finalNewName2 = newName;
        // StepVerifier.create(subjectRepository.updateAllById(subjectEntity))
        //     .expectNextMatches(newSubjectEntity
        //     -> finalNewName2.equals(newSubjectEntity.getName())
        //         && nameCn.equals(newSubjectEntity.getNameCn()))
        //     .verifyComplete();
    }

    @Test
    void template() {
        final String name = "test" + new Random(100).nextInt();
        SubjectEntity subjectEntity = SubjectEntity.builder()
            .name(name)
            .type(SubjectType.ANIME)
            .nsfw(false)
            .build();

        StepVerifier.create(template.insert(subjectEntity))
            .expectNext(subjectEntity).verifyComplete();

        StepVerifier.create(
                template.select(Query.query(where("name").is(name)), SubjectEntity.class)
            )
            .expectNext(subjectEntity)
            .verifyComplete();

    }
}