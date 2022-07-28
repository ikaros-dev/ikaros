package cn.liguohao.ikaros.persistence.incompact.file;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class FileDataOperateResult {

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

    private FileData fileData;

    private Throwable throwable;

    private FileDataOperateResult() {
    }

    private FileDataOperateResult(Status status, String msg, FileData fileData,
                                  Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.fileData = fileData;
        this.throwable = throwable;
    }

    private FileDataOperateResult(Status status, String msg, Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.throwable = throwable;
    }

    private FileDataOperateResult(Status status, FileData fileData, Throwable throwable) {
        this.status = status;
        this.fileData = fileData;
        this.throwable = throwable;
    }

    /**
     * 操作正常，状态OK
     *
     * @param msg 提示信息, 可以是上传成功的路径
     * @return 结果实例
     */
    public static FileDataOperateResult ofOk(String msg) {
        return new FileDataOperateResult(Status.OK, msg,  null);
    }

    public static FileDataOperateResult ofOk(FileData fileData) {
        return new FileDataOperateResult(Status.OK, fileData, null);
    }

    /**
     * @see #ofOk(String)
     */
    public static FileDataOperateResult ofOk() {
        return ofOk("subject data operate ok.");
    }

    public static FileDataOperateResult ofNotFound(String msg) {
        return new FileDataOperateResult(Status.DATA_NOT_FOUND,
            " appoint location subject data not found, msg=" + msg, null);
    }

    public static FileDataOperateResult ofUploadFail(String location) {
        return ofUploadFail(location, null);
    }

    public static FileDataOperateResult ofUploadFail(String msg, Throwable throwable) {
        return new FileDataOperateResult(Status.UPLOAD_FAIL,
            " appoint location subject data upload fail, msg=" + msg, throwable);
    }

    public static FileDataOperateResult ofDeleteFail(String msg, Throwable throwable) {
        return new FileDataOperateResult(Status.DELETE_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static FileDataOperateResult ofDownloadFail(String msg, Throwable throwable) {
        return new FileDataOperateResult(Status.DOWNLOAD_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static FileDataOperateResult ofException(String msg, Throwable throwable) {
        return new FileDataOperateResult(Status.HAS_EXCEPTION,
            " appoint location operate has exception, msg=" + msg, throwable);
    }


    public Status status() {
        return status;
    }

    public String msg() {
        return msg;
    }

    public FileData itemData() {
        return fileData;
    }

    public Throwable throwable() {
        return throwable;
    }
}
