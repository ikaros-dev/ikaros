package run.ikaros.reading; import java.util.UUID; public record ReadingVolumeView(UUID id,UUID editionId,String kind,String displayLabel,String structuredNumber,int sortOrder,String title) {}
