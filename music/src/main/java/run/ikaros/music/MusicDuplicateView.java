package run.ikaros.music;
import java.util.UUID;
public record MusicDuplicateView(UUID attachmentId,UUID trackId,String title,String sha256,long sizeBytes) {}
