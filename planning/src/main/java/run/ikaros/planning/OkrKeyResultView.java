package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record OkrKeyResultView(UUID id,UUID ownerId,UUID objectiveId,String title,OkrMetricType metricType,Double startValue,Double targetValue,Double currentValue,OkrStatus status,Instant createdAt,Instant updatedAt,long version) {}
