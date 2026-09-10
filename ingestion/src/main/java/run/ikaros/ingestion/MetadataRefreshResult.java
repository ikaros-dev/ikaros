package run.ikaros.ingestion;

public record MetadataRefreshResult(String status, MetadataCandidateView candidate) { }
