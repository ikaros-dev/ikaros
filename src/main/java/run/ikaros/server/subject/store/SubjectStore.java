package run.ikaros.server.subject.store;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import run.ikaros.server.subject.store.entity.MetadataEntity;
import run.ikaros.server.subject.store.entity.SpecificationEntity;
import run.ikaros.server.subject.store.entity.StatusEntity;
import run.ikaros.server.subject.store.entity.SubjectEntity;

/**
 * SubjectStore is built-up entities or storing Subject data into database.
 *
 * @author: li-guohao
 */
public record SubjectStore(@Nonnull SubjectEntity subject,
                           @Nullable List<MetadataEntity> metadataList,
                           @Nullable List<SpecificationEntity> specificationList,
                           @Nullable List<StatusEntity> statusList) {
}
