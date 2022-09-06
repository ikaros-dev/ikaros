package cn.liguohao.ikaros.file;


public interface IkarosFileHandler {

    IkarosFileOperateResult upload(IkarosFile ikarosFile);

    IkarosFileOperateResult download(IkarosFile ikarosFile);

    IkarosFileOperateResult delete(IkarosFile ikarosFile);

    boolean exist(String uploadedPath);

}
