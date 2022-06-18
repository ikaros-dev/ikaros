package cn.liguohao.ikaros.subject;

/**
 * 定义条目数据与文件系统交互的处理器
 *
 * @author li-guohao
 * @date 2022/06/18
 */
public interface SubjectDataHandler {

    /**
     * 上传条目数据
     *
     * @param subjectData 待上传的条目数据
     * @return 条目数据的操作结果
     */
    SubjectDataOperateResult upload(SubjectData subjectData);

    /**
     * 下载条目数据
     *
     * @param subjectData 条目数据
     * @return 条目数据的操作结果，如果成功的则包含条目数据
     */
    SubjectDataOperateResult download(SubjectData subjectData);


    /**
     * 删除条目数据
     *
     * @param subjectData 条目数据
     * @return 条目数据的操作结果
     */
    SubjectDataOperateResult delete(SubjectData subjectData);

}
