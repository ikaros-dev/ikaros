package run.ikaros.server.core.service;

import run.ikaros.server.entity.SubscribeEntity;

import javax.annotation.Nonnull;

public interface SubscribeService extends CrudService<SubscribeEntity, Long> {

    void subscribeBgmTvSubject(@Nonnull Long bgmTvSubjectId);
}
