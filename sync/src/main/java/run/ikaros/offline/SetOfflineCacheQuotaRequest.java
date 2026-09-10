package run.ikaros.offline;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetOfflineCacheQuotaRequest(@NotNull UUID deviceId, @Min(1) long quotaBytes) {}
