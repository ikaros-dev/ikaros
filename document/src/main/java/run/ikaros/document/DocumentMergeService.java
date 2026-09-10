package run.ikaros.document;

final class DocumentMergeService {
    private DocumentMergeService() {}

    static MergeWorkingCopyView merge(String serverContent, String baseContent, String localContent, String schema, long version) {
        if (serverContent.equals(baseContent)) return new MergeWorkingCopyView(localContent, false, version, schema);
        if (localContent.equals(baseContent) || serverContent.equals(localContent)) return new MergeWorkingCopyView(serverContent, false, version, schema);
        String merged = "<<<<<<< LOCAL\n" + localContent + "\n=======\n" + serverContent + "\n>>>>>>> SERVER";
        return new MergeWorkingCopyView(merged, true, version, schema);
    }
}
