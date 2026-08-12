package run.ikaros.api.core.subject;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectOperate extends AllowPluginOperate {

    Mono<Subject> findById(UUID id);

    Flux<Subject> findAllByPageable(PagingWrap<Subject> pagingWrap);

    Mono<Subject> create(Subject subject);

    Mono<Void> update(Subject subject);


    Mono<Void> syncByPlatform(@Nullable UUID subjectId, SubjectSyncPlatform platform,
                                 String platformId);


    Mono<Subject> findBySubjectIdAndPlatformAndPlatformId(@NonNull UUID subjectId,
                                                          @NonNull
                                                          SubjectSyncPlatform subjectSyncPlatform,
                                                          @NotBlank String platformId);

    Flux<Subject> findByPlatformAndPlatformId(@NonNull SubjectSyncPlatform subjectSyncPlatform,
                                              @NotBlank String platformId);

    Mono<Boolean> existsByPlatformAndPlatformId(@NonNull SubjectSyncPlatform subjectSyncPlatform,
                                                @NotBlank String platformId);
}
