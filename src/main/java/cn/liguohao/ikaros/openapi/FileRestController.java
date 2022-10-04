package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.common.result.PagingWrap;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.entity.FileEntity;
import cn.liguohao.ikaros.model.vo.FileVO;
import cn.liguohao.ikaros.service.FileService;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
        Assert.notNull(multipartFile, "'file' must not be null");
        Assert.notNull(multipartFile.getBytes(), "file bytes must not be null");
        Assert.isTrue(multipartFile.getBytes().length > 0, "file bytes must >0");
        Optional<FileEntity> fileEntityOptional =
            fileService.upload(multipartFile.getOriginalFilename(), multipartFile.getBytes());
        return CommonResult.ok(fileEntityOptional.orElseGet(null));
    }

    @GetMapping("/{id}")
    public CommonResult<FileEntity> findById(@PathVariable Long id) throws RecordNotFoundException {
        Assert.notNull(id, "'id' must not be null");
        FileEntity fileEntity = fileService.findById(id);
        return CommonResult.ok(fileEntity);
    }

    @DeleteMapping("/{id}")
    public CommonResult<FileEntity> deleteById(@PathVariable Long id) {
        Assert.notNull(id, "'id' must not be null");
        fileService.delete(id);
        return CommonResult.ok("delete file entity success for id: " + id);
    }

    @PutMapping("/data/{id}")
    public CommonResult<FileEntity> update(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile multipartFile)
        throws IOException, RecordNotFoundException {
        Assert.notNull(id, "'id' must not be null");
        Assert.notNull(multipartFile, "'file' must not be null");
        Assert.notNull(multipartFile.getBytes(), "file bytes must not be null");
        Assert.isTrue(multipartFile.getBytes().length > 0, "file bytes must >0");
        FileEntity fileEntity = fileService.update(id, multipartFile);
        return CommonResult.ok(fileEntity);
    }

    @PostMapping
    public CommonResult<FileEntity> update(FileEntity fileEntity) {
        Assert.notNull(fileEntity, "'fileEntity' must not be null");
        return CommonResult.ok(fileService.update(fileEntity));
    }

    @GetMapping("/list")
    public CommonResult<PagingWrap<FileVO>> listPaging() {
        // todo impl
        return CommonResult.ok();
    }
}
