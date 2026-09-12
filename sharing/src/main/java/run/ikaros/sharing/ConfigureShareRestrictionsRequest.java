package run.ikaros.sharing;

public record ConfigureShareRestrictionsRequest(
    String password,
    Boolean allowDownload,
    Integer maxAccessCount) {}
