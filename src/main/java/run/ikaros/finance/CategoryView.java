package run.ikaros.finance; import java.util.UUID; public record CategoryView(UUID id,UUID ledgerId,UUID parentId,String name,String kind,boolean archived) {}
