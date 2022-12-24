package run.ikaros.server.openapi;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.bind.annotation.RequestBody;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.params.SearchFilesParams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author guohao
 * @date 2022/09/07
 */
@Slf4j
@Tag(name = "文件")
@RestController
@RequestMapping("/file")
public class FileRestController {
    private final FileService fileService;

    public FileRestController(FileService fileService) {
        this.fileService = fileService;
    }


    /**
     * 上传文件
     *
     * @param multipartFile 文件内容
     * @return 文件信息
     * @throws IOException IO
     */
    @PutMapping("/data")
    public CommonResult<FileEntity> upload(@RequestParam("file") MultipartFile multipartFile)
        throws IOException {
        AssertUtils.notNull(multipartFile, "'file' must not be null");
        AssertUtils.notNull(multipartFile.getBytes(), "file bytes must not be null");
        AssertUtils.isTrue(multipartFile.getBytes().length > 0, "file bytes must >0");
        FileEntity fileEntity =
            fileService.upload(multipartFile.getOriginalFilename(), multipartFile.getBytes());
        return CommonResult.ok(fileEntity);
    }

    @GetMapping("/{id}")
    public CommonResult<FileEntity> findById(@PathVariable Long id) throws RecordNotFoundException {
        AssertUtils.notNull(id, "'id' must not be null");
        return CommonResult.ok(fileService.findById(id));
    }


    @DeleteMapping
    public CommonResult<Object> deleteByIds(HttpServletRequest request) {
        Collection<String[]> values = request.getParameterMap().values();
        Set<Long> ids = new HashSet<>();
        for (String[] value : values) {
            ids.add(Long.valueOf(value[0]));
        }
        if (ids.size() > 0) {
            fileService.deleteInBatch(ids);
        }

        return CommonResult.ok("delete file entity success for ids: ");
    }

    @PutMapping("/data/{id}")
    public CommonResult<FileEntity> update(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile multipartFile)
        throws IOException, RecordNotFoundException {
        AssertUtils.notNull(id, "'id' must not be null");
        AssertUtils.notNull(multipartFile, "'file' must not be null");
        AssertUtils.notNull(multipartFile.getBytes(), "file bytes must not be null");
        AssertUtils.isTrue(multipartFile.getBytes().length > 0, "file bytes must >0");
        FileEntity fileEntity = fileService.update(id, multipartFile);
        return CommonResult.ok(fileEntity);
    }

    @PostMapping
    public CommonResult<FileEntity> update(FileEntity fileEntity) {
        AssertUtils.notNull(fileEntity, "'fileEntity' must not be null");
        return CommonResult.ok(fileService.update(fileEntity));
    }

    @PostMapping("/list")
    public CommonResult<PagingWrap<FileEntity>> listPaging(
        @RequestBody SearchFilesParams searchFilesParams) {
        return CommonResult.ok(fileService.findFilesByPagingAndCondition(searchFilesParams));
    }

    @GetMapping("/list/name/{name}")
    public CommonResult<List<FileEntity>> findListByName(@PathVariable("name") String name) {
        return CommonResult.ok(fileService.findListByName(name));
    }

    @GetMapping("/types")
    public CommonResult<Set<String>> findTypes() {
        return CommonResult.ok(fileService.findTypes());
    }

    @GetMapping("/places")
    public CommonResult<Set<String>> findPlaces() {
        return CommonResult.ok(fileService.findPlaces());
    }

    @PutMapping("/name")
    public CommonResult<Object> updateNameById(String name, Long id)
        throws RecordNotFoundException {
        fileService.updateNameById(name, id);
        return CommonResult.ok();
    }

    @PostMapping("/filepond/unique")
    public String getUploadUniqueLocation() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @PatchMapping("/filepond/patch/{unique}")
    public void receiveChunkFile(@PathVariable("unique") String unique,
                                 HttpServletRequest request) throws IOException {
        String uploadLength = request.getHeader("Upload-Length");
        String uploadOffset = request.getHeader("Upload-Offset");
        String uploadName = request.getHeader("Upload-Name");
        uploadName =
            new String(Base64.getDecoder()
                .decode(uploadName.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        try {
            fileService.receiveAndHandleChunkFile(unique, uploadLength, uploadOffset, uploadName,
                request.getInputStream().readAllBytes());
        } catch (ClientAbortException clientAbortException) {
            log.debug("upload file abort, unique={}", unique);
        }
    }

    @DeleteMapping("/filepond/revert")
    public void revertUploadFileByUnique(@RequestBody String unique) {
        fileService.revertUploadChunkFileAndDir(unique);
    }


}
