package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.JacksonConverter;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.common.kit.FileKit;
import cn.liguohao.ikaros.common.kit.SystemVarKit;
import cn.liguohao.ikaros.common.kit.TimeKit;
import cn.liguohao.ikaros.common.result.PagingWrap;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.entity.FileEntity;
import cn.liguohao.ikaros.model.file.IkarosFile;
import cn.liguohao.ikaros.model.file.IkarosFileHandler;
import cn.liguohao.ikaros.model.file.IkarosFileOperateResult;
import cn.liguohao.ikaros.model.file.LocalIkarosFileHandler;
import cn.liguohao.ikaros.model.param.SearchFilesParams;
import cn.liguohao.ikaros.repository.FileRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
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
    private final Environment environment;
    private IkarosFileHandler fileHandler = new LocalIkarosFileHandler();

    public FileService(FileRepository fileRepository, Environment environment) {
        this.fileRepository = fileRepository;
        this.environment = environment;
    }

    public Optional<FileEntity> upload(String originalFilename, byte[] bytes) throws IOException {
        Assert.notNull(originalFilename, "'originalFilename' must not bo null");
        Assert.notNull(bytes, "'bytes' must not bo null");

        IkarosFile ikarosFile = IkarosFile.build(originalFilename, bytes);
        fileHandler.upload(ikarosFile);

        FileEntity fileEntity = getFileEntity(bytes, ikarosFile, null);

        fileEntity = fileRepository.save(fileEntity);

        return Optional.of(fileEntity);
    }

    private FileEntity getFileEntity(byte[] bytes, IkarosFile ikarosFile,
                                     @Nullable FileEntity fileEntity) {
        Assert.notNull(bytes, "'bytes' must not bo null");
        Assert.notNull(ikarosFile, "'ikarosFile' must not bo null");
        final int size = bytes.length;
        Assert.isTrue(size > 0, "'bytes' length must > 0");

        final String md5 = FileKit.checksum2Str(bytes, FileKit.Hash.MD5);
        final String sha256 = FileKit.checksum2Str(bytes, FileKit.Hash.SHA256);

        Date uploadedDate = TimeKit.localDataTime2Date(ikarosFile.getUploadedTime());
        if (fileEntity == null) {
            fileEntity = new FileEntity();
        }

        IkarosFile.Place place = ikarosFile.getPlace();
        // 如果存储位置是本地，则对应的URL格式为 http://ip:port/upload/xxx.jpg
        String uploadedPath = ikarosFile.getUploadedPath();
        String url = "";
        if (place == IkarosFile.Place.LOCAL) {
            String currentAppDirPath = SystemVarKit.getCurrentAppDirPath();
            String ipAddress = SystemVarKit.getIPAddress();
            String port = environment.getProperty("local.server.port");
            String baseUrl = "http://" + ipAddress + ":" + port;
            url = uploadedPath.replace(currentAppDirPath, baseUrl);
            // 如果是ntfs目录URL，则需要替换下 \ 为 /
            if (url.indexOf("\\") > 0) {
                url = url.replace("\\", "/");
            }
        } else {
            // 其它情况下，url和uploadedPath相同
            url = uploadedPath;
        }

        fileEntity
            .setLocation(uploadedPath)
            .setUrl(url)
            .setMd5(md5)
            .setSha256(sha256)
            .setSize(size)
            .setName(ikarosFile.getName())
            .setPostfix(ikarosFile.getPostfix())
            .setType(ikarosFile.getType())
            .setPlace(place)
            .setCreateTime(uploadedDate)
            .setUpdateTime(uploadedDate);
        return fileEntity;
    }

    public FileEntity findById(Long fileId) throws RecordNotFoundException {
        Assert.notNull(fileId, "'fileId' must not be null");
        Assert.isPositive(fileId);
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
        Assert.isPositive(fileId);

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
        Assert.isPositive(fileId);
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

        existFileEntity = getFileEntity(bytes, ikarosFile, existFileEntity);
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
        Assert.isNotBlank(name, "'name' must not be null");

        FileEntity existFileEntity = findById(id);
        existFileEntity.setName(name);
        return fileRepository.saveAndFlush(existFileEntity);
    }
}
