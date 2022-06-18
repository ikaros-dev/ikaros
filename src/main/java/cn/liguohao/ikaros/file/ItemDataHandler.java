package cn.liguohao.ikaros.file;

/**
 * 定义条目数据项与文件系统交互的处理器
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public interface ItemDataHandler {

    /**
     * 上传条目数据项
     *
     * @param itemData 待上传的条目数据项
     * @return 条目数据项的操作结果
     */
    ItemDataOperateResult upload(ItemData itemData);

    /**
     * 下载条目数据项
     *
     * @param itemData 条目数据项
     * @return 条目数据项的操作结果，如果成功的则包含条目数据项
     */
    ItemDataOperateResult download(ItemData itemData);


    /**
     * 删除条目数据项
     *
     * @param itemData 条目数据项
     * @return 条目数据项的操作结果
     */
    ItemDataOperateResult delete(ItemData itemData);

}
