package run.ikaros.server.core.file;

import java.util.List;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.entity.MetadataEntity;

public record File(FileEntity entity, List<MetadataEntity> metadata) {
}
