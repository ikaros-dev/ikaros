package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.SettingEntity;

/**
 * SettingEntity repository.
 *
 * @author: li-guohao
 * @see SettingEntity
 */
public interface SettingRepository extends R2dbcRepository<SettingEntity, Long> {
}
