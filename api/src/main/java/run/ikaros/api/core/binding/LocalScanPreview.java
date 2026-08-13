package run.ikaros.api.core.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

/** 本地目录无副作用扫描后的预览结果. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LocalScanPreview {
    /** 被扫描目录的附件标识. */
    @JsonProperty("directory_id")
    private @Nullable UUID directoryId;
    /** 本次预览使用的媒体扫描模式. */
    private @Nullable LocalMediaMode mode;
    /** 按目录顺序返回的扫描项. */
    private @Nullable List<LocalScanItem> items;
}
