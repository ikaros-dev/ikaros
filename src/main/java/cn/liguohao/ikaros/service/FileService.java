package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.FileKit;
import cn.liguohao.ikaros.common.TimeKit;
import cn.liguohao.ikaros.entity.FileEntity;
import cn.liguohao.ikaros.file.IkarosFile;
import cn.liguohao.ikaros.file.IkarosFileHandler;
import cn.liguohao.ikaros.file.LocalIkarosFileHandler;
import cn.liguohao.ikaros.repository.FileRepository;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author guohao
 * @date 2022/09/07
 */
@Service
public class FileService {

    private final FileRepository fileRepository;
    private IkarosFileHandler fileHandler = new LocalIkarosFileHandler();

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public Optional<FileEntity> upload(String originalFilename, byte[] bytes) throws IOException {
        Assert.notNull(originalFilename, "'originalFilename' must not bo null");
        Assert.notNull(bytes, "'bytes' must not bo null");

        int size = bytes.length;
        Assert.isTrue(size > 0, "'bytes' length must > 0");

        IkarosFile ikarosFile = IkarosFile.build(originalFilename, bytes);
        fileHandler.upload(ikarosFile);

        String md5 = FileKit.checksum2Str(bytes, FileKit.Hash.MD5);
        String sha256 = FileKit.checksum2Str(bytes, FileKit.Hash.SHA256);

        Date uploadedDate = TimeKit.localDataTime2Date(ikarosFile.getUploadedTime());
        FileEntity fileEntity = (FileEntity) new FileEntity()
            .setLocation(ikarosFile.getUploadedPath())
            .setMd5(md5)
            .setSha256(sha256)
            .setSize(size)
            .setName(ikarosFile.getName())
            .setPostfix(ikarosFile.getPostfix())
            .setCreteTime(uploadedDate)
            .setUpdateTime(uploadedDate);

        fileEntity = fileRepository.save(fileEntity);

        return Optional.of(fileEntity);
    }

    public Optional<FileEntity> download(String localtion) {


        return null;
    }

    public FileEntity update(FileEntity fileEntity) {


        return null;
    }

    public FileEntity update(FileEntity fileEntity, MultipartFile multipartFile) {


        return null;
    }

    public void delete(Long fileId) {

    }
}
