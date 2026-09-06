package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record OkrCheckInView(UUID id,UUID ownerId,UUID keyResultId,Double currentValue,Double progress,OkrConfidence confidence,String note,String blocker,Instant createdAt) {}
