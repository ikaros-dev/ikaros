package run.ikaros.planning; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
public record CreateOkrKeyResultRequest(@NotBlank String title,@NotNull OkrMetricType metricType,@NotNull Double startValue,@NotNull Double targetValue) {}
