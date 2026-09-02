package run.ikaros.drive;
import java.util.UUID;
public record DriveQuotaView(UUID spaceId, long limitBytes, long usedBytes, long reservedBytes,
    long availableBytes) {}
