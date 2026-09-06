package run.ikaros.storage;

import java.util.List;

record StorageRestorePartialSelection(List<String> attachmentIds, long totalBytes,
    StorageRestoreBudgetDecision budgetDecision) {
    StorageRestorePartialSelection {
        attachmentIds = List.copyOf(attachmentIds);
    }
}
