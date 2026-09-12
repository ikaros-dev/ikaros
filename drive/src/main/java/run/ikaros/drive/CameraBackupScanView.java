package run.ikaros.drive;

import java.util.List;

public record CameraBackupScanView(List<CameraBackupView> discovered, List<CameraBackupView> known) {}
