package run.ikaros.storage.api;

/** 生命周期受 Storage 管理的临时上传会话状态。 */
public enum UploadSessionState {
    OPEN,
    RECEIVING,
    FINALIZING,
    COMPLETED,
    ABORTED,
    EXPIRED
}
