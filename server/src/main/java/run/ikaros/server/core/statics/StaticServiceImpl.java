package run.ikaros.server.core.statics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.infra.properties.IkarosProperties;

@Slf4j
@Service
public class StaticServiceImpl implements StaticService {
    private final IkarosProperties ikarosProperties;

    public StaticServiceImpl(IkarosProperties ikarosProperties) {
        this.ikarosProperties = ikarosProperties;
    }

    @Override
    public Flux<String> listStaticsFonts() {
        final String fontBaseUrl = "/static/" + AppConst.STATIC_FONT_DIR_NAME + '/';
        Path staticsDirPath = ikarosProperties.getWorkDir().resolve(AppConst.STATIC_DIR_NAME);
        if (Files.notExists(staticsDirPath)) {
            try {
                Files.createDirectory(staticsDirPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return Flux.empty();
        }
        Path staticsFontsDirPath = staticsDirPath.resolve(AppConst.STATIC_FONT_DIR_NAME);
        if (Files.notExists(staticsFontsDirPath)) {
            try {
                Files.createDirectory(staticsFontsDirPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return Flux.empty();
        }

        File[] files = staticsFontsDirPath.toFile().listFiles();

        if (Objects.isNull(files)) {
            return Flux.empty();
        }

        return Flux.fromStream(Arrays.stream(files))
            .map(File::getName)
            .map(name -> fontBaseUrl + name);
    }
}
