package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.JacksonConverter;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.common.kit.FileKit;
import cn.liguohao.ikaros.common.kit.TimeKit;
import cn.liguohao.ikaros.common.result.PagingWrap;
import cn.liguohao.ikaros.exceptions.NotSupportRuntimeException;
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
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
            .setType(ikarosFile.getType())
            .setCreateTime(uploadedDate)
            .setUpdateTime(uploadedDate);

        fileEntity = fileRepository.save(fileEntity);

        return Optional.of(fileEntity);
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
        final String md5 = FileKit.checksum2Str(bytes, FileKit.Hash.MD5);
        final String sha256 = FileKit.checksum2Str(bytes, FileKit.Hash.SHA256);
        ikarosFile.setMd5(md5);
        ikarosFile.setSha256(sha256);
        if (Strings.isNotBlank(oldLocation)) {
            ikarosFile.setOldLocation(oldLocation);
        }

        IkarosFileOperateResult uploadResult = fileHandler.upload(ikarosFile);
        ikarosFile = uploadResult.getIkarosFile();
        final String uploadedPath = ikarosFile.getUploadedPath();
        final Date uploadedDate = TimeKit.localDataTime2Date(ikarosFile.getUploadedTime());
        if (IkarosFileOperateResult.Status.OK == uploadResult.getStatus()) {
            LOGGER.info("success upload file for path: {}", uploadedPath);
        }

        existFileEntity.setLocation(uploadedPath);
        existFileEntity.setUpdateTime(uploadedDate);
        existFileEntity.setSize(bytes.length);
        existFileEntity.setPostfix(ikarosFile.getPostfix());
        existFileEntity.setType(ikarosFile.getType());
        existFileEntity.setName(ikarosFile.getName());
        existFileEntity.setMd5(md5);
        existFileEntity.setSha256(sha256);
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
        Specification<FileEntity> queryCondition = new Specification<FileEntity>() {
            @Override
            public Predicate toPredicate(Root<FileEntity> root, CriteriaQuery<?> criteriaQuery,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();
                if (Strings.isNotBlank(keyword)) {
                    //predicateList.add(criteriaBuilder.equal(root.get("name"), keyword));
                    throw new NotSupportRuntimeException("not support search by keyword");
                }
                if (Strings.isNotBlank(type)) {
                    String postfix = type.substring(type.lastIndexOf("/") + 1);
                    predicateList.add(criteriaBuilder.equal(root.get("postfix"), postfix));
                }
                if (Strings.isNotBlank(place)) {
                    predicateList.add(criteriaBuilder.equal(root.get("place"), place));
                }
                Predicate[] predicates = new Predicate[predicateList.size()];
                return criteriaBuilder.and(predicateList.toArray(predicates));
            }
        };

        // 分页和不分页，这里按起始页和每页展示条数为0时默认为不分页，分页的话按创建时间降序
        if (pageIndex == null || pageSize == null || (pageIndex == 0 && pageSize == 0)) {
            fileEntities = fileRepository.findAll(queryCondition);
        } else {
            fileEntities = fileRepository.findAll(queryCondition,
                    PageRequest.of(pageIndex - 1, pageSize,
                        Sort.by(Sort.Direction.DESC, "createTime")))
                .getContent();
        }

        return new PagingWrap<FileEntity>()
            .setContent(fileEntities)
            .setCurrentIndex(pageIndex)
            .setTotal(fileRepository.count(queryCondition));
    }
}
