package run.ikaros.server.core.meta;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.meta.MetaInfoExtensionPoint;
import run.ikaros.api.core.subject.SubjectRecord;
import run.ikaros.api.infra.utils.AssertUtils;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.plugin.ExtensionComponentsFinder;

@Slf4j
@Service
public class DefaultMetaInfoService implements MetaInfoService {

    private final ExtensionComponentsFinder extensionComponentsFinder;

    public DefaultMetaInfoService(ExtensionComponentsFinder extensionComponentsFinder) {
        this.extensionComponentsFinder = extensionComponentsFinder;
    }

    @Override
    public Flux<SubjectRecord> searchSubjects(SubjectSyncPlatform platform, String keyword) {
        AssertUtils.notNull(platform, "'platform' must not be null.");
        AssertUtils.notBlank(keyword, "'keyword' must not blank.");
        return findByPlatform(platform)
            .flatMapMany(ext -> ext.searchSubjects(keyword));
    }

    @Override
    public Mono<SubjectRecord> getSubjectByPlatformId(
        SubjectSyncPlatform platform, String platformId) {
        AssertUtils.notNull(platform, "'platform' must not be null.");
        AssertUtils.notBlank(platformId, "'platformId' must not blank.");
        return findByPlatform(platform)
            .flatMap(ext -> ext.getSubjectByPlatformId(platformId));
    }

    private Mono<MetaInfoExtensionPoint> findByPlatform(SubjectSyncPlatform platform) {
        List<MetaInfoExtensionPoint> extensions =
            extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class);
        return Mono.justOrEmpty(extensions.stream()
            .filter(ext -> ext.getPlatform() == platform)
            .findFirst()
        ).switchIfEmpty(Mono.error(new IllegalStateException(
            "No MetaInfoExtensionPoint found for platform: " + platform)));
    }
}
