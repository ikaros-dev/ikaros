package run.ikaros.music; import jakarta.validation.constraints.NotNull; import java.util.List; import java.util.UUID; public record CreateMusicQueueRequest(@NotNull List<UUID> trackIds) {}
