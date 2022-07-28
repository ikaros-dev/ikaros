package cn.liguohao.ikaros.persistence.incompact.file;

/**
 * 定义条目数据项与文件系统交互的处理器
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public interface FileDataHandler {

    /**
     * 上传条目数据项
     *
     * @param fileData 待上传的条目数据项
     * @return 条目数据项的操作结果
     */
    FileDataOperateResult upload(FileData fileData);

    /**
     * 下载条目数据项
     *
     * @param fileData 条目数据项
     * @return 条目数据项的操作结果，如果成功的则包含条目数据项
     */
    FileDataOperateResult download(FileData fileData);


    /**
     * 删除条目数据项
     *
     * @param fileData 条目数据项
     * @return 条目数据项的操作结果
     */
    FileDataOperateResult delete(FileData fileData);

    /**
     * 路径的项数据是否存在
     *
     * @param uploadedPath 上传后的路径
     * @return true-存在 false-不存在
     */
    boolean exist(String uploadedPath);

}
