package run.ikaros.notes; import java.time.Instant; import java.util.List; public record PrivateNoteSyncView(List<NoteView> notes,Instant nextCursor) {}
