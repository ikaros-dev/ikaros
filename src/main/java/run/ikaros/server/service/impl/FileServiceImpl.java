package run.ikaros.server.service.impl;

import java.util.HashSet;
import javax.annotation.Nonnull;
import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.FileType;
import run.ikaros.server.service.FileService;
import run.ikaros.server.service.base.AbstractCrudService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.FileUtils;
import run.ikaros.server.utils.SystemVarUtils;
import run.ikaros.server.utils.TimeUtils;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.file.IkarosFile;
import run.ikaros.server.file.IkarosFileHandler;
import run.ikaros.server.file.IkarosFileOperateResult;
import run.ikaros.server.file.LocalIkarosFileHandler;
import run.ikaros.server.params.SearchFilesParams;
import run.ikaros.server.prop.IkarosProperties;
import run.ikaros.server.repository.FileRepository;
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
public class FileServiceImpl
    extends AbstractCrudService<FileEntity, Long>
    implements FileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileRepository fileRepository;
    private final IkarosProperties ikarosProperties;
    private final IkarosFileHandler fileHandler = new LocalIkarosFileHandler();

    public FileServiceImpl(FileRepository fileRepository, IkarosProperties ikarosProperties) {
        super(fileRepository);
        this.fileRepository = fileRepository;
        this.ikarosProperties = ikarosProperties;
    }

    @Nonnull
    @Override
    public FileEntity upload(@Nonnull String originalFilename, @Nonnull byte[] bytes) {
        AssertUtils.notNull(originalFilename, "'originalFilename' must not bo null");
        AssertUtils.notNull(bytes, "'bytes' must not bo null");

        IkarosFile ikarosFile = IkarosFile.build(originalFilename, bytes);
        try {
            FileEntity fileEntity = uploadAndGetFileEntity(bytes, ikarosFile, null);
            return fileRepository.saveAndFlush(fileEntity);
        } catch (IOException ioException) {
            throw new RuntimeIkarosException(
                "upload file fail, originalFilename=" + originalFilename);
        }
    }

    private FileEntity uploadAndGetFileEntity(byte[] bytes, IkarosFile ikarosFile,
                                              @Nullable FileEntity fileEntity) throws IOException {
        AssertUtils.notNull(bytes, "'bytes' must not bo null");
        AssertUtils.notNull(ikarosFile, "'ikarosFile' must not bo null");
        final int size = bytes.length;
        AssertUtils.isTrue(size > 0, "'bytes' length must > 0");

        final String md5 = FileUtils.checksum2Str(bytes, FileUtils.Hash.MD5);
        ikarosFile.setMd5(md5);

        if (fileEntity == null) {
            fileEntity = new FileEntity();
        }

        // 如果数据库存在相同的文件，则不进行重复上传
        List<FileEntity> sameMd5FileEntities = fileRepository.findByMd5(md5);
        if (sameMd5FileEntities != null && sameMd5FileEntities.size() > 0) {
            FileEntity sameMd5FileEntity = sameMd5FileEntities.get(0);
            String oldLocation = sameMd5FileEntity.getUrl();
            ikarosFile
                .setUploadedPath(oldLocation)
                .setOldLocation(oldLocation);
        } else {
            // upload file to file system
            IkarosFileOperateResult fileOperateResult = fileHandler.upload(ikarosFile);
            ikarosFile = fileOperateResult.getIkarosFile();
        }

        FilePlace place = ikarosFile.getPlace();
        Date uploadedDate = TimeUtils.localDataTime2Date(ikarosFile.getUploadedTime());
        // 如果存储位置是本地，则对应的URL格式为 http://ip:port/upload/xxx.jpg
        String uploadedPath = ikarosFile.getUploadedPath();
        String url = "";
        if (place == FilePlace.LOCAL) {
            url = path2url(uploadedPath);
        } else {
            // 其它情况下，url和uploadedPath相同
            url = uploadedPath;
        }

        fileEntity
            .setUrl(uploadedPath)
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

    @Nonnull
    @Override
    public FileEntity findById(@Nonnull Long fileId) {
        AssertUtils.isPositive(fileId, "'fileId' must be positive");
        Optional<FileEntity> fileEntityOptional = fileRepository.findByIdAndStatus(fileId, true);
        if (fileEntityOptional.isEmpty()) {
            throw new RecordNotFoundException("record not found, fileId: " + fileId);
        }
        return fileEntityOptional.get();
    }

    @Override
    public void delete(@Nonnull Long fileId) {
        try {
            FileEntity fileEntity = findById(fileId);
            final String location = fileEntity.getUrl();
            if (StringUtils.isNotBlank(location)) {
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

    @Nonnull
    @Override
    public FileEntity update(@Nonnull FileEntity fileEntity) {
        AssertUtils.notNull(fileEntity, "'fileEntity' must not be null.");
        Long fileId = fileEntity.getId();
        AssertUtils.isPositive(fileId, "'fileId' must be positive");

        FileEntity existFileEntity = null;
        try {
            existFileEntity = findById(fileId);
            // 已经存在，则更新原有的
            final String oldExistFileEntityJson = JsonUtils.obj2Json(existFileEntity);
            BeanUtils.copyProperties(fileEntity, existFileEntity);
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

    @Nonnull
    @Override
    public FileEntity update(@Nonnull Long fileId, @Nonnull MultipartFile multipartFile)
        throws IOException {
        AssertUtils.isPositive(fileId, "'fileId' must be positive");
        AssertUtils.notNull(multipartFile, "'multipartFile' must not be null");

        // 更新部分数据
        FileEntity existFileEntity = null;
        String oldLocation = null;
        try {
            existFileEntity = findById(fileId);

            // 如果旧的文件存在，则移除旧的文件
            oldLocation = existFileEntity.getUrl();
        } catch (RecordNotFoundException e) {
            // 没有旧的数据，则新增一条
            // 查询ID是否已经存在
            FileEntity idExistFileEntity = null;
            try {
                idExistFileEntity = fileRepository.getById(fileId);
                idExistFileEntity.setStatus(true);
                oldLocation = idExistFileEntity.getUrl();
            } catch (EntityNotFoundException entityNotFoundException) {
                idExistFileEntity = new FileEntity();
                // ps: 这里的新的保存的ID并不是指定的ID
            }
            existFileEntity = fileRepository.saveAndFlush(idExistFileEntity);
        }


        // 上传文件
        String originalFilename = multipartFile.getOriginalFilename();
        byte[] bytes = multipartFile.getBytes();
        AssertUtils.notNull(originalFilename, "'originalFilename' mus not be null");
        AssertUtils.notNull(bytes, "'bytes' must not be null.");
        AssertUtils.isTrue(bytes.length > 0, "'bytes' length must > 0");

        IkarosFile ikarosFile = IkarosFile.build(originalFilename, bytes);
        if (StringUtils.isNotBlank(oldLocation)) {
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
        if (StringUtils.isNotBlank(location) && fileHandler.exist(location)) {
            IkarosFileOperateResult operateResult = fileHandler.delete(location);
            if (IkarosFileOperateResult.Status.OK == operateResult.getStatus()) {
                LOGGER.info("success delete file for path: {}", location);
            }
        }
    }

    @Nonnull
    @Override
    public PagingWrap<FileEntity> findFilesByPagingAndCondition(
        @Nonnull SearchFilesParams searchFilesParams) {
        AssertUtils.notNull(searchFilesParams, "'searchFilesParams' must not be null");
        final Integer pageIndex = searchFilesParams.getPage();
        final Integer pageSize = searchFilesParams.getSize();
        final String keyword = searchFilesParams.getKeyword();
        String originType = searchFilesParams.getType();
        final String place = searchFilesParams.getPlace();
        String type = null;
        String postfix = null;

        String[] typeArr = null;
        if (StringUtils.isNotBlank(originType)) {
            typeArr = originType.split("/");
            if (typeArr.length != 2) {
                throw new IllegalArgumentException(
                    "illegal type: " + originType + "  format example: image/png");
            }
        }

        if (typeArr != null) {
            type = typeArr[0];
            postfix = typeArr[1];
        }


        if (pageIndex != null) {
            AssertUtils.isPositive(pageIndex, "'page' must not be negative");
        }
        if (pageSize != null) {
            AssertUtils.isPositive(pageSize, "'size' must not be negative");
        }

        List<FileEntity> fileEntities = null;

        // 构造自定义查询条件
        final String finalType = type;
        final String finalPostfix = postfix;
        Specification<FileEntity> queryCondition = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            // 过滤掉逻辑删除的
            predicateList.add(criteriaBuilder.equal(root.get("status"), true));

            if (StringUtils.isNotBlank(keyword)) {
                predicateList.add(criteriaBuilder.like(root.get("name"), "%" + keyword + "%"));
            }

            if (StringUtils.isNotBlank(finalType)) {
                predicateList.add(
                    criteriaBuilder.equal(root.get("type"), FileType.valueOf(finalType)));
            }
            if (StringUtils.isNotBlank(finalPostfix)) {
                predicateList.add(
                    criteriaBuilder.equal(root.get("postfix"), FileType.valueOf(finalPostfix)));
            }
            if (StringUtils.isNotBlank(place)) {
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
    @Nonnull
    @Override
    public Set<String> findTypes() {
        Set<String> types = fileRepository.findTypes();
        Set<String> postfixSet = fileRepository.findPostfix();

        Set<String> resultSet = new HashSet<>(types.size() * postfixSet.size());
        for (String type : types) {
            type = type.toLowerCase();
            for (String postfix : postfixSet) {
                postfix = postfix.startsWith(".") ? postfix.substring(1) : postfix;
                resultSet.add(type + "/" + postfix);
            }
        }

        return resultSet;
    }

    @Nonnull
    @Override
    public Set<String> findPlaces() {
        return fileRepository.findPlaces();
    }

    @Override
    public void deleteInBatch(@Nonnull Set<Long> ids) {
        AssertUtils.notNull(ids, "'ids' must not be null");
        for (Long id : ids) {
            delete(id);
        }
    }

    @Nonnull
    @Override
    public FileEntity updateNameById(@Nonnull String name, @Nonnull Long id) {
        AssertUtils.notNull(id, "'id' must not be null");
        AssertUtils.notBlank(name, "'name' must not be blank");

        FileEntity existFileEntity = findById(id);
        existFileEntity.setName(name);
        return fileRepository.saveAndFlush(existFileEntity);
    }

    @Override
    public void receiveAndHandleChunkFile(@Nonnull String unique, @Nonnull String uploadLength,
                                          @Nonnull String uploadOffset, @Nonnull String uploadName,
                                          @Nonnull byte[] bytes) throws IOException {
        AssertUtils.notNull(unique, "'unique' must not be null");
        AssertUtils.notNull(uploadLength, "'uploadLength' must not be null");
        AssertUtils.notNull(uploadOffset, "'uploadOffset' must not be null");
        AssertUtils.notNull(uploadName, "'uploadName' must not be null");
        AssertUtils.notNull(bytes, "'bytes' must not be null");

        File tempChunkFileCacheDir =
            new File(SystemVarUtils.getOsCacheDirPath() + File.separator + unique);
        if (!tempChunkFileCacheDir.exists()) {
            tempChunkFileCacheDir.mkdirs();
            LOGGER.debug("create temp dir: {}", tempChunkFileCacheDir);
        }

        AssertUtils.notNull(bytes, "file bytes must not be null");

        long offset = Long.parseLong(uploadOffset) + bytes.length;
        File uploadedChunkCacheFile = new File(tempChunkFileCacheDir + File.separator + offset);
        Files.write(Path.of(uploadedChunkCacheFile.toURI()), bytes);
        LOGGER.debug("upload chunk[{}] to path: {}", uploadOffset,
            uploadedChunkCacheFile.getAbsolutePath());

        if (offset == Long.parseLong(uploadLength)) {
            String postfix = uploadName.substring(uploadName.lastIndexOf(".") + 1);
            final String filePath = meringTempChunkFile(unique, postfix);

            for (File file : tempChunkFileCacheDir.listFiles()) {
                file.delete();
            }
            tempChunkFileCacheDir.delete();

            uploadName = uploadName.substring(0, uploadName.lastIndexOf("."));
            FileEntity fileEntity = (FileEntity) new FileEntity()
                .setMd5(FileUtils.checksum2Str(bytes, FileUtils.Hash.MD5))
                .setUrl(filePath)
                .setPlace(FilePlace.LOCAL)
                .setUrl(path2url(filePath))
                .setName(uploadName)
                .setPostfix(postfix)
                .setSize(Integer.valueOf(uploadLength))
                .setType(FileUtils.parseTypeByPostfix(postfix))
                .setCreateTime(new Date())
                .setUpdateTime(new Date());

            fileRepository.saveAndFlush(fileEntity);
        }
    }

    private String meringTempChunkFile(String unique, String postfix) throws IOException {
        LOGGER.debug("All chunks upload has finish, will start merging files");

        File targetFile = new File(FileUtils.buildAppUploadFilePath(postfix));
        String absolutePath = targetFile.getAbsolutePath();

        String chunkFileDirPath = SystemVarUtils.getOsCacheDirPath() + File.separator + unique;
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
        String currentAppDirPath = SystemVarUtils.getCurrentAppDirPath();
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
