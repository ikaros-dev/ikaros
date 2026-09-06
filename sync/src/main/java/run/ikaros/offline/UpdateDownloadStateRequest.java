package run.ikaros.offline;
import jakarta.validation.constraints.NotNull;
public record UpdateDownloadStateRequest(@NotNull DownloadState state, String failureReason) {}
