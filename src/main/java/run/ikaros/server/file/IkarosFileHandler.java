package run.ikaros.server.file;


import java.io.IOException;

public interface IkarosFileHandler {

    IkarosFileOperateResult upload(IkarosFile ikarosFile) throws IOException;

    IkarosFileOperateResult download(IkarosFile ikarosFile);

    IkarosFileOperateResult delete(IkarosFile ikarosFile);

    IkarosFileOperateResult delete(String uploadedPath);

    boolean exist(String uploadedPath);

    IkarosFile.Place getPlace();

}
