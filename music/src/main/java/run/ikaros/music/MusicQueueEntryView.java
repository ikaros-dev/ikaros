package run.ikaros.music; import java.util.UUID; public record MusicQueueEntryView(UUID id,UUID queueId,UUID trackId,int basePosition,int activePosition) {}
