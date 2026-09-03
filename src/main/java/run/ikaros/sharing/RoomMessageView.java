package run.ikaros.sharing; import java.time.Instant; import java.util.UUID; public record RoomMessageView(UUID id,UUID roomId,UUID authorId,String body,Instant createdAt) {}
