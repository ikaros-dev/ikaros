package run.ikaros.server.file;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class IkarosFileOperateResult {

    public enum Status {
        /**
         * 一切正常，操作OK。
         */
        OK,
        /**
         * 没有找到条目数据
         */
        DATA_NOT_FOUND,
        /**
         * 上传失败
         */
        UPLOAD_FAIL,
        /**
         * 删除失败
         */
        DELETE_FAIL,

        /**
         * 下载失败
         */
        DOWNLOAD_FAIL,

        /**
         * 出现了异常
         */
        HAS_EXCEPTION
        ;
    }

    private Status status;

    private String msg;

    private IkarosFile ikarosFile;

    private Throwable throwable;

    private IkarosFileOperateResult() {
    }

    private IkarosFileOperateResult(Status status, String msg, IkarosFile ikarosFile,
                                    Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.ikarosFile = ikarosFile;
        this.throwable = throwable;
    }

    private IkarosFileOperateResult(Status status, String msg, Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.throwable = throwable;
    }

    private IkarosFileOperateResult(Status status, IkarosFile ikarosFile, Throwable throwable) {
        this.status = status;
        this.ikarosFile = ikarosFile;
        this.throwable = throwable;
    }

    /**
     * 操作正常，状态OK
     *
     * @param msg 提示信息, 可以是上传成功的路径
     * @return 结果实例
     */
    public static IkarosFileOperateResult ofOk(String msg) {
        return new IkarosFileOperateResult(Status.OK, msg,  null);
    }

    public static IkarosFileOperateResult ofOk(IkarosFile ikarosFile) {
        return new IkarosFileOperateResult(Status.OK, ikarosFile, null);
    }

    /**
     * @see #ofOk(String)
     */
    public static IkarosFileOperateResult ofOk() {
        return ofOk("subject data operate ok.");
    }

    public static IkarosFileOperateResult ofNotFound(String msg) {
        return new IkarosFileOperateResult(Status.DATA_NOT_FOUND,
            " appoint location subject data not found, msg=" + msg, null);
    }

    public static IkarosFileOperateResult ofUploadFail(String location) {
        return ofUploadFail(location, null);
    }

    public static IkarosFileOperateResult ofUploadFail(String msg, Throwable throwable) {
        return new IkarosFileOperateResult(Status.UPLOAD_FAIL,
            " appoint location subject data upload fail, msg=" + msg, throwable);
    }

    public static IkarosFileOperateResult ofDeleteFail(String msg, Throwable throwable) {
        return new IkarosFileOperateResult(Status.DELETE_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static IkarosFileOperateResult ofDownloadFail(String msg, Throwable throwable) {
        return new IkarosFileOperateResult(Status.DOWNLOAD_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static IkarosFileOperateResult ofException(String msg, Throwable throwable) {
        return new IkarosFileOperateResult(Status.HAS_EXCEPTION,
            " appoint location operate has exception, msg=" + msg, throwable);
    }

    public Status getStatus() {
        return status;
    }

    public IkarosFileOperateResult setStatus(
        Status status) {
        this.status = status;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public IkarosFileOperateResult setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public IkarosFile getIkarosFile() {
        return ikarosFile;
    }

    public IkarosFileOperateResult setIkarosFile(IkarosFile ikarosFile) {
        this.ikarosFile = ikarosFile;
        return this;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public IkarosFileOperateResult setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }
}
