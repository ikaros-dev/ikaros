package run.ikaros.storage;

public enum StorageRestoreBudgetAction {
    REJECT,
    REQUIRE_CONFIRMATION,
    QUEUE_AFTER_BUDGET_RESET,
    PARTIAL_ACCEPT
}
