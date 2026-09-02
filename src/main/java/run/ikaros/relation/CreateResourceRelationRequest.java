package run.ikaros.relation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 建立 Resource 关系时接受的目标、类型和显示顺序。
 */
public record CreateResourceRelationRequest(@NotNull UUID targetResourceId, @NotNull ResourceRelationType type,
                                            Integer position) {
}
