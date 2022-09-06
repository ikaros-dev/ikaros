package cn.liguohao.ikaros.file;

/**
 * 定义条目数据项与文件系统交互的处理器
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public interface IkarosFileHandler {

    /**
     * 上传条目数据项
     *
     * @param ikarosFile 待上传的条目数据项
     * @return 条目数据项的操作结果
     */
    IkarosFileOperateResult upload(IkarosFile ikarosFile);

    /**
     * 下载条目数据项
     *
     * @param ikarosFile 条目数据项
     * @return 条目数据项的操作结果，如果成功的则包含条目数据项
     */
    IkarosFileOperateResult download(IkarosFile ikarosFile);


    /**
     * 删除条目数据项
     *
     * @param ikarosFile 条目数据项
     * @return 条目数据项的操作结果
     */
    IkarosFileOperateResult delete(IkarosFile ikarosFile);

    /**
     * 路径的项数据是否存在
     *
     * @param uploadedPath 上传后的路径
     * @return true-存在 false-不存在
     */
    boolean exist(String uploadedPath);

}
