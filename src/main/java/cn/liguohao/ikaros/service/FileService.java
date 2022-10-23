package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.JacksonConverter;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.common.kit.FileKit;
import cn.liguohao.ikaros.common.kit.SystemVarKit;
import cn.liguohao.ikaros.common.kit.TimeKit;
import cn.liguohao.ikaros.common.result.PagingWrap;
import cn.liguohao.ikaros.exceptions.IkarosRuntimeException;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.entity.FileEntity;
import cn.liguohao.ikaros.model.file.IkarosFile;
import cn.liguohao.ikaros.model.file.IkarosFileHandler;
import cn.liguohao.ikaros.model.file.IkarosFileOperateResult;
import cn.liguohao.ikaros.model.file.LocalIkarosFileHandler;
import cn.liguohao.ikaros.model.param.SearchFilesParams;
import cn.liguohao.ikaros.prop.IkarosProperties;
import cn.liguohao.ikaros.repository.FileRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author guohao
 * @date 2022/09/07
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class FileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final IkarosProperties ikarosProperties;
    private IkarosFileHandler fileHandler = new LocalIkarosFileHandler();

    public FileService(FileRepository fileRepository, IkarosProperties ikarosProperties) {
        this.fileRepository = fileRepository;
        this.ikarosProperties = ikarosProperties;
    }

    public FileEntity upload(String originalFilename, byte[] bytes) {
        Assert.notNull(originalFilename, "'originalFilename' must not bo null");
        Assert.notNull(bytes, "'bytes' must not bo null");

        IkarosFile ikarosFile = IkarosFile.build(originalFilename, bytes);
        try {
            FileEntity fileEntity = uploadAndGetFileEntity(bytes, ikarosFile, null);
            return fileRepository.saveAndFlush(fileEntity);
        } catch (IOException ioException) {
            throw new IkarosRuntimeException(
                "upload file fail, originalFilename=" + originalFilename);
        }
    }

    private FileEntity uploadAndGetFileEntity(byte[] bytes, IkarosFile ikarosFile,
                                              @Nullable FileEntity fileEntity) throws IOException {
        Assert.notNull(bytes, "'bytes' must not bo null");
        Assert.notNull(ikarosFile, "'ikarosFile' must not bo null");
        final int size = bytes.length;
        Assert.isTrue(size > 0, "'bytes' length must > 0");

        final String md5 = FileKit.checksum2Str(bytes, FileKit.Hash.MD5);
        ikarosFile.setMd5(md5);

        if (fileEntity == null) {
            fileEntity = new FileEntity();
        }

        // 如果数据库存在相同的文件，则不进行重复上传
        List<FileEntity> sameMd5FileEntities = fileRepository.findByMd5(md5);
        if (sameMd5FileEntities != null && sameMd5FileEntities.size() > 0) {
            FileEntity sameMd5FileEntity = sameMd5FileEntities.get(0);
            String oldLocation = sameMd5FileEntity.getLocation();
            ikarosFile
                .setUploadedPath(oldLocation)
                .setOldLocation(oldLocation);
        } else {
            // upload file to file system
            IkarosFileOperateResult fileOperateResult = fileHandler.upload(ikarosFile);
            ikarosFile = fileOperateResult.getIkarosFile();
        }

        IkarosFile.Place place = ikarosFile.getPlace();
        Date uploadedDate = TimeKit.localDataTime2Date(ikarosFile.getUploadedTime());
        // 如果存储位置是本地，则对应的URL格式为 http://ip:port/upload/xxx.jpg
        String uploadedPath = ikarosFile.getUploadedPath();
        String url = "";
        if (place == IkarosFile.Place.LOCAL) {
            url = path2url(uploadedPath);
        } else {
            // 其它情况下，url和uploadedPath相同
            url = uploadedPath;
        }

        fileEntity
            .setLocation(uploadedPath)
            .setUrl(url)
            .setMd5(md5)
            .setSize(size)
            .setName(ikarosFile.getName())
            .setPostfix(ikarosFile.getPostfix())
            .setType(ikarosFile.getType())
            .setPlace(place)
            .setCreateTime(uploadedDate)
            .setUpdateTime(uploadedDate);

        return fileRepository.saveAndFlush(fileEntity);
    }

    public FileEntity findById(Long fileId) throws RecordNotFoundException {
        Assert.isPositive(fileId, "'fileId' must be positive");
        Optional<FileEntity> fileEntityOptional = fileRepository.findByIdAndStatus(fileId, true);
        if (fileEntityOptional.isEmpty()) {
            throw new RecordNotFoundException("record not found, fileId: " + fileId);
        }
        return fileEntityOptional.get();
    }


    public void delete(Long fileId) {
        try {
            FileEntity fileEntity = findById(fileId);
            final String location = fileEntity.getLocation();
            if (Strings.isNotBlank(location)) {
                // 由于目前是逻辑删除，暂时不删除文件
                //fileHandler.delete(location);
            }
            fileEntity.setStatus(false);
            fileRepository.saveAndFlush(fileEntity);

            LOGGER.info("delete file entity, fileId={}", fileId);
        } catch (RecordNotFoundException e) {
            LOGGER.debug("not exist file entity for fileId={}, do nothing.", fileId);
        }
    }

    public FileEntity update(FileEntity fileEntity) {
        Assert.notNull(fileEntity, "'fileEntity' must not be null.");
        Long fileId = fileEntity.getId();
        Assert.isPositive(fileId, "'fileId' must be positive");

        FileEntity existFileEntity = null;
        try {
            existFileEntity = findById(fileId);
            // 已经存在，则更新原有的
            final String oldExistFileEntityJson = JacksonConverter.obj2Json(existFileEntity);
            BeanKit.copyProperties(fileEntity, existFileEntity);
            existFileEntity = fileRepository.saveAndFlush(existFileEntity);
            LOGGER.info("success update exist file entity, old: {} , new: {}",
                oldExistFileEntityJson, existFileEntity);
        } catch (RecordNotFoundException e) {
            // 不存在，则新增
            LOGGER.debug("not exist file entity for fileId={}, do nothing.", fileId);
            existFileEntity = fileRepository.saveAndFlush(fileEntity);
            LOGGER.info("success add file entity: {}", existFileEntity);
        }

        return existFileEntity;
    }

    public FileEntity update(Long fileId, MultipartFile multipartFile)
        throws IOException {
        Assert.isPositive(fileId, "'fileId' must be positive");
        Assert.notNull(multipartFile, "'multipartFile' must not be null");

        // 更新部分数据
        FileEntity existFileEntity = null;
        String oldLocation = null;
        try {
            existFileEntity = findById(fileId);

            // 如果旧的文件存在，则移除旧的文件
            oldLocation = existFileEntity.getLocation();
        } catch (RecordNotFoundException e) {
            // 没有旧的数据，则新增一条
            // 查询ID是否已经存在
            FileEntity idExistFileEntity = null;
            try {
                idExistFileEntity = fileRepository.getById(fileId);
                idExistFileEntity.setStatus(true);
                oldLocation = idExistFileEntity.getLocation();
            } catch (EntityNotFoundException entityNotFoundException) {
                idExistFileEntity = new FileEntity();
                // ps: 这里的新的保存的ID并不是指定的ID
            }
            existFileEntity = fileRepository.saveAndFlush(idExistFileEntity);
        }


        // 上传文件
        String originalFilename = multipartFile.getOriginalFilename();
        byte[] bytes = multipartFile.getBytes();
        Assert.notNull(originalFilename, "'originalFilename' mus not be null");
        Assert.notNull(bytes, "'bytes' must not be null.");
        Assert.isTrue(bytes.length > 0, "'bytes' length must > 0");

        IkarosFile ikarosFile = IkarosFile.build(originalFilename, bytes);
        if (Strings.isNotBlank(oldLocation)) {
            ikarosFile.setOldLocation(oldLocation);
        }

        IkarosFileOperateResult uploadResult = fileHandler.upload(ikarosFile);
        if (IkarosFileOperateResult.Status.OK == uploadResult.getStatus()) {
            LOGGER.info("success upload file for path: {}", ikarosFile.getUploadedPath());
        }
        ikarosFile = uploadResult.getIkarosFile();

        existFileEntity = uploadAndGetFileEntity(bytes, ikarosFile, existFileEntity);
        existFileEntity = fileRepository.saveAndFlush(existFileEntity);
        return existFileEntity;
    }

    private void deleteIfExist(String location) {
        if (Strings.isNotBlank(location) && fileHandler.exist(location)) {
            IkarosFileOperateResult operateResult = fileHandler.delete(location);
            if (IkarosFileOperateResult.Status.OK == operateResult.getStatus()) {
                LOGGER.info("success delete file for path: {}", location);
            }
        }
    }

    public PagingWrap<FileEntity> findFilesByPagingAndCondition(
        SearchFilesParams searchFilesParams) {
        Assert.notNull(searchFilesParams, "'searchFilesParams' must not be null");
        Integer pageIndex = searchFilesParams.getPage();
        Integer pageSize = searchFilesParams.getSize();
        String keyword = searchFilesParams.getKeyword();
        String type = searchFilesParams.getType();
        String place = searchFilesParams.getPlace();

        if (pageIndex != null) {
            Assert.isPositive(pageIndex, "'page' must not be negative");
        }
        if (pageSize != null) {
            Assert.isPositive(pageSize, "'size' must not be negative");
        }

        List<FileEntity> fileEntities = null;

        // 构造自定义查询条件
        Specification<FileEntity> queryCondition = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            // 过滤掉逻辑删除的
            predicateList.add(criteriaBuilder.equal(root.get("status"), true));

            if (Strings.isNotBlank(keyword)) {
                predicateList.add(criteriaBuilder.like(root.get("name"), "%" + keyword + "%"));
            }
            if (Strings.isNotBlank(type)) {
                predicateList.add(
                    criteriaBuilder.equal(root.get("type"), IkarosFile.Type.valueOf(type)));
            }
            if (Strings.isNotBlank(place)) {
                predicateList.add(
                    criteriaBuilder.equal(root.get("place"), IkarosFile.Place.valueOf(place)));
            }
            Predicate[] predicates = new Predicate[predicateList.size()];
            return criteriaBuilder.and(predicateList.toArray(predicates));
        };

        // 分页和不分页，这里按起始页和每页展示条数为0时默认为不分页，分页的话按创建时间降序
        if (pageIndex == null || pageSize == null || (pageIndex == 0 && pageSize == 0)) {
            fileEntities = fileRepository.findAll(queryCondition);
        } else {
            // page小于1时，都为第一页, page从1开始，即第一页 pageIndex=1
            fileEntities = fileRepository.findAll(queryCondition,
                    PageRequest.of(pageIndex < 1 ? 0 : (pageIndex - 1), pageSize,
                        Sort.by(Sort.Direction.DESC, "createTime")))
                .getContent();
        }

        return new PagingWrap<FileEntity>()
            .setContent(fileEntities)
            .setCurrentIndex(pageIndex)
            .setTotal(fileRepository.count(queryCondition));
    }

    /**
     * @return 所有的文件类型
     */
    public Set<String> findTypes() {
        return fileRepository.findTypes();
    }

    public Set<String> findPlaces() {
        return fileRepository.findPlaces();
    }

    public void deleteInBatch(Set<Long> ids) {
        Assert.notNull(ids, "'ids' must not be null");
        for (Long id : ids) {
            delete(id);
        }
    }

    public FileEntity updateNameById(String name, Long id) throws RecordNotFoundException {
        Assert.notNull(id, "'id' must not be null");
        Assert.notBlank(name, "'name' must not be blank");

        FileEntity existFileEntity = findById(id);
        existFileEntity.setName(name);
        return fileRepository.saveAndFlush(existFileEntity);
    }

    public void receiveAndHandleChunkFile(String unique, String uploadLength, String uploadOffset,
                                          String uploadName, byte[] bytes) throws IOException {
        Assert.notNull(unique, "'unique' must not be null");
        Assert.notNull(uploadLength, "'uploadLength' must not be null");
        Assert.notNull(uploadOffset, "'uploadOffset' must not be null");
        Assert.notNull(uploadName, "'uploadName' must not be null");
        Assert.notNull(bytes, "'bytes' must not be null");

        File tempChunkFileCacheDir =
            new File(SystemVarKit.getOsCacheDirPath() + File.separator + unique);
        if (!tempChunkFileCacheDir.exists()) {
            tempChunkFileCacheDir.mkdirs();
            LOGGER.debug("create temp dir: {}", tempChunkFileCacheDir);
        }

        Assert.notNull(bytes, "file bytes must not be null");

        Long offset = Long.parseLong(uploadOffset) + bytes.length;
        File uploadedChunkCacheFile = new File(tempChunkFileCacheDir + File.separator + offset);
        Files.write(Path.of(uploadedChunkCacheFile.toURI()), bytes);
        LOGGER.debug("upload chunk[{}] to path: {}", uploadOffset,
            uploadedChunkCacheFile.getAbsolutePath());

        if (offset == Long.parseLong(uploadLength)) {
            String postfix = uploadName.substring(uploadName.lastIndexOf(".") + 1);
            String filePath = meringTempChunkFile(unique, postfix);

            for (File file : tempChunkFileCacheDir.listFiles()) {
                file.delete();
            }
            tempChunkFileCacheDir.delete();

            uploadName = uploadName.substring(0, uploadName.lastIndexOf("."));
            FileEntity fileEntity = (FileEntity) new FileEntity()
                .setMd5(FileKit.checksum2Str(bytes, FileKit.Hash.MD5))
                .setLocation(filePath)
                .setPlace(IkarosFile.Place.LOCAL)
                .setUrl(path2url(filePath))
                .setName(uploadName)
                .setSize(Integer.valueOf(uploadLength))
                .setPostfix(postfix)
                .setType(FileKit.parseTypeByPostfix(postfix))
                .setCreateTime(new Date())
                .setUpdateTime(new Date());

            fileRepository.saveAndFlush(fileEntity);
        }
    }

    private String meringTempChunkFile(String unique, String postfix) throws IOException {
        LOGGER.debug("All chunks upload has finish, will start merging files");

        File targetFile = new File(FileKit.buildAppUploadFilePath(postfix));
        String absolutePath = targetFile.getAbsolutePath();

        String chunkFileDirPath = SystemVarKit.getOsCacheDirPath() + File.separator + unique;
        File chunkFileDir = new File(chunkFileDirPath);
        File[] files = chunkFileDir.listFiles();
        List<File> chunkFileList = Arrays.asList(files);
        // PS: 这里需要根据文件名(偏移量)升序, 不然合并的文件分片内容的顺序不正常
        Collections.sort(chunkFileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long o1Offset = Long.parseLong(o1.getName());
                long o2Offset = Long.parseLong(o2.getName());
                if (o1Offset < o2Offset) {
                    return -1;
                } else if (o1Offset > o2Offset) {
                    return 1;
                }
                return 0;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }
        });
        int targetFileWriteOffset = 0;
        for (File chunkFile : chunkFileList) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "rw");
                 FileInputStream fileInputStream = new FileInputStream(chunkFile);) {
                randomAccessFile.seek(targetFileWriteOffset);
                byte[] bytes = new byte[fileInputStream.available()];
                int read = fileInputStream.read(bytes);
                randomAccessFile.write(bytes);
                targetFileWriteOffset += read;
                LOGGER.debug("[{}] current merge targetFileWriteOffset: {}", chunkFile.getName(),
                    targetFileWriteOffset);
            }
        }

        LOGGER.debug("Merging all chunk files success, absolute path: {}", absolutePath);
        return absolutePath;
    }

    private String path2url(String path) {
        String url = "";
        String currentAppDirPath = SystemVarKit.getCurrentAppDirPath();
        path = path.startsWith("//") ? path.substring(1) : path;
        // issue #50
        url = currentAppDirPath.startsWith("/") ? path : path.replace(currentAppDirPath, "");
        // 如果是开发环境，需要加上 http://ip:port
        if (ikarosProperties.envIsDev()) {
            url = ikarosProperties.getServerHttpBaseUrl() + url;
        }

        // 如果是ntfs目录，则需要替换下 \ 为 /
        if (url.indexOf("\\") > 0) {
            url = url.replace("\\", "/");
        }
        return url;
    }
}
