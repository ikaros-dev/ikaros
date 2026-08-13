package run.ikaros.api.core.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

/** 媒体文件中探测到的内嵌轨道信息. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MediaTrack {
    /** 外置轨道对应的附件标识，内嵌轨道为空. */
    @JsonProperty("attachment_id")
    private @Nullable UUID attachmentId;
    /** 外置轨道的受控播放地址，内嵌轨道为空. */
    private @Nullable String url;
    /** 轨道在媒体容器中的序号. */
    private @Nullable Integer index;
    /** 轨道类别. */
    private @Nullable String kind;
    /** 轨道语言. */
    private @Nullable String language;
    /** 轨道标题. */
    private @Nullable String title;
    /** 是否为默认轨道. */
    @JsonProperty("default_track")
    private boolean defaultTrack;
    /** 轨道编码格式. */
    private @Nullable String codec;
    /** 当前浏览器是否可播放该轨道. */
    private boolean playable;
    /** 无法探测或播放时的原因. */
    @JsonProperty("failure_reason")
    private @Nullable String failureReason;
}
