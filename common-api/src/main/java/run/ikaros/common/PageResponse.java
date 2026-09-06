package run.ikaros.common;

import java.util.List;

/** 统一分页响应，页码从零开始。 */
public record PageResponse<T>(List<T> items, long total, int page, int size) {
}
