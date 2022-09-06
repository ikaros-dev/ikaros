package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.entity.FileEntity;
import cn.liguohao.ikaros.service.FileService;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/test")
    public String test() {
        return "hello";
    }

    /**
     * 上传文件
     *
     * @param multipartFile 文件内容
     * @return 文件信息
     * @throws IOException IO
     */
    @PostMapping("/upload")
    public FileEntity upload(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        Optional<FileEntity> fileEntityOptional =
            fileService.upload(multipartFile.getOriginalFilename(), multipartFile.getBytes());
        return fileEntityOptional.orElseGet(null);
    }
}
