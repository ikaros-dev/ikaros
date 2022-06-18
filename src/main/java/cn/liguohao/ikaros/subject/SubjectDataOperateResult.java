package cn.liguohao.ikaros.subject;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class SubjectDataOperateResult {

    enum Status {
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

    private SubjectData subjectData;

    private Throwable throwable;

    private SubjectDataOperateResult() {
    }

    private SubjectDataOperateResult(Status status, String msg, SubjectData subjectData,
                                    Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.subjectData = subjectData;
        this.throwable = throwable;
    }

    private SubjectDataOperateResult(Status status, String msg, Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.throwable = throwable;
    }

    private SubjectDataOperateResult(Status status, SubjectData subjectData, Throwable throwable) {
        this.status = status;
        this.subjectData = subjectData;
        this.throwable = throwable;
    }

    /**
     * 操作正常，状态OK
     *
     * @param msg 提示信息, 可以是上传成功的路径
     * @return 结果实例
     */
    public static SubjectDataOperateResult ofOk(String msg) {
        return new SubjectDataOperateResult(Status.OK, msg,  null);
    }

    public static SubjectDataOperateResult ofOk(SubjectData subjectData) {
        return new SubjectDataOperateResult(Status.OK, subjectData, null);
    }

    /**
     * @see #ofOk(String)
     */
    public static SubjectDataOperateResult ofOk() {
        return ofOk("subject data operate ok.");
    }

    public static SubjectDataOperateResult ofNotFound(String msg) {
        return new SubjectDataOperateResult(Status.DATA_NOT_FOUND,
            " appoint location subject data not found, msg=" + msg, null);
    }

    public static SubjectDataOperateResult ofUploadFail(String location) {
        return ofUploadFail(location, null);
    }

    public static SubjectDataOperateResult ofUploadFail(String msg, Throwable throwable) {
        return new SubjectDataOperateResult(Status.UPLOAD_FAIL,
            " appoint location subject data upload fail, msg=" + msg, throwable);
    }

    public static SubjectDataOperateResult ofDeleteFail(String msg, Throwable throwable) {
        return new SubjectDataOperateResult(Status.DELETE_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static SubjectDataOperateResult ofDownloadFail(String msg, Throwable throwable) {
        return new SubjectDataOperateResult(Status.DOWNLOAD_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static SubjectDataOperateResult ofException(String msg, Throwable throwable) {
        return new SubjectDataOperateResult(Status.HAS_EXCEPTION,
            " appoint location operate has exception, msg=" + msg, throwable);
    }


    public Status status() {
        return status;
    }

    public String msg() {
        return msg;
    }

    public SubjectData subjectData() {
        return subjectData;
    }

    public Throwable throwable() {
        return throwable;
    }
}
