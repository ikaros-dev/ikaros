package run.ikaros.photo; import java.time.LocalDate; import java.util.List; public record PhotoTimelineGroupView(LocalDate date,List<PhotoView> photos) {}
