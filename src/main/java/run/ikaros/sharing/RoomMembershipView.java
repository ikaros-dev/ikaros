package run.ikaros.sharing; import java.time.Instant; import java.util.UUID; public record RoomMembershipView(UUID id,UUID roomId,UUID principalId,RoomRole role,Instant joinedAt,Instant leftAt) {}
