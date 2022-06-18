package cn.liguohao.ikaros.file;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class ItemDataOperateResult {

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

    private ItemData itemData;

    private Throwable throwable;

    private ItemDataOperateResult() {
    }

    private ItemDataOperateResult(Status status, String msg, ItemData itemData,
                                  Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.itemData = itemData;
        this.throwable = throwable;
    }

    private ItemDataOperateResult(Status status, String msg, Throwable throwable) {
        this.status = status;
        this.msg = msg;
        this.throwable = throwable;
    }

    private ItemDataOperateResult(Status status, ItemData itemData, Throwable throwable) {
        this.status = status;
        this.itemData = itemData;
        this.throwable = throwable;
    }

    /**
     * 操作正常，状态OK
     *
     * @param msg 提示信息, 可以是上传成功的路径
     * @return 结果实例
     */
    public static ItemDataOperateResult ofOk(String msg) {
        return new ItemDataOperateResult(Status.OK, msg,  null);
    }

    public static ItemDataOperateResult ofOk(ItemData itemData) {
        return new ItemDataOperateResult(Status.OK, itemData, null);
    }

    /**
     * @see #ofOk(String)
     */
    public static ItemDataOperateResult ofOk() {
        return ofOk("subject data operate ok.");
    }

    public static ItemDataOperateResult ofNotFound(String msg) {
        return new ItemDataOperateResult(Status.DATA_NOT_FOUND,
            " appoint location subject data not found, msg=" + msg, null);
    }

    public static ItemDataOperateResult ofUploadFail(String location) {
        return ofUploadFail(location, null);
    }

    public static ItemDataOperateResult ofUploadFail(String msg, Throwable throwable) {
        return new ItemDataOperateResult(Status.UPLOAD_FAIL,
            " appoint location subject data upload fail, msg=" + msg, throwable);
    }

    public static ItemDataOperateResult ofDeleteFail(String msg, Throwable throwable) {
        return new ItemDataOperateResult(Status.DELETE_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static ItemDataOperateResult ofDownloadFail(String msg, Throwable throwable) {
        return new ItemDataOperateResult(Status.DOWNLOAD_FAIL,
            " appoint location subject data delete fail, msg=" + msg, throwable);
    }

    public static ItemDataOperateResult ofException(String msg, Throwable throwable) {
        return new ItemDataOperateResult(Status.HAS_EXCEPTION,
            " appoint location operate has exception, msg=" + msg, throwable);
    }


    public Status status() {
        return status;
    }

    public String msg() {
        return msg;
    }

    public ItemData subjectData() {
        return itemData;
    }

    public Throwable throwable() {
        return throwable;
    }
}
