package run.ikaros.server.core.subject.enums;

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
