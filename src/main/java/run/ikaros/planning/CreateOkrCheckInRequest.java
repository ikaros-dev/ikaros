package run.ikaros.planning;
import jakarta.validation.constraints.NotNull;
public record CreateOkrCheckInRequest(@NotNull Double currentValue,@NotNull OkrConfidence confidence,String note,String blocker) {}
