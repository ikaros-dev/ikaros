package run.ikaros.server.core.collection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.store.entity.SubjectCollectionEntity;
import run.ikaros.server.store.repository.SubjectCollectionRepository;

@Slf4j
@Service
public class DefaultCollectionService implements CollectionService {
    private final SubjectCollectionRepository subjectCollectionRepository;

    private final UserService userService;

    public DefaultCollectionService(SubjectCollectionRepository subjectCollectionRepository,
                                    UserService userService) {
        this.subjectCollectionRepository = subjectCollectionRepository;
        this.userService = userService;
    }

    @Override
    public Mono<CollectionType> findTypeBySubjectId(Long subjectId) {
        Assert.isTrue(subjectId > 0, "subjectId must be greater than 0");
        return userService.getUserIdFromSecurityContext()
            .flatMap(uid -> subjectCollectionRepository.findByUserIdAndSubjectId(uid, subjectId))
            .map(SubjectCollectionEntity::getType);
    }
}
