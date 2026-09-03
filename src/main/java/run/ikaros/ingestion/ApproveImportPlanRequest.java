package run.ikaros.ingestion;
import jakarta.validation.constraints.NotNull;
public record ApproveImportPlanRequest(@NotNull Long expectedVersion) { }
