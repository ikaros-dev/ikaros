package run.ikaros.progress;

import jakarta.validation.constraints.Min;

/**
 * 设置消费进度的请求。
 *
 * @param type 进度单位类型
 * @param position 当前进度值
 * @param total 可选总进度值
 * @param completed 是否完成
 */
public record SetProgressRequest(
    ProgressType type,
    @Min(0) long position,
    @Min(1) Long total,
    boolean completed
) {
}
