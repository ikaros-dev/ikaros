package run.ikaros.api.core.subject;

public enum SubjectSyncAction {
    /**
     * will override all subject meta info.
     */
    PULL,
    /**
     * will update meta info that absent.
     */
    MERGE
}
