package run.ikaros.game; import java.time.Instant; import java.util.UUID; public record GameVersionView(UUID id,UUID gameId,UUID platformId,String versionLabel,Instant releaseDate) {}
