package run.ikaros.drive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CameraBackupScanRequest(@NotEmpty @Size(max = 200) List<@Valid CameraBackupScanItem> items) {}
