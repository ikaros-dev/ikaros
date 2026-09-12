package run.ikaros.sharing;

public record ShareRestrictionView(
    boolean passwordRequired,
    boolean allowDownload,
    Integer maxAccessCount,
    int accessCount) {}
