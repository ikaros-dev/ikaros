package run.ikaros.document;

import java.util.List;

public record RevisionComparisonView(long fromRevisionNumber, long toRevisionNumber, List<RevisionDiffLine> lines) {}
