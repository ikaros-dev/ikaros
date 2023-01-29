package run.ikaros.server.core.file;

import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import run.ikaros.server.custom.ReactiveCustomClient;
import run.ikaros.server.custom.scheme.SchemeInitializedEvent;

@Component
public class FileInitializer implements ApplicationListener<SchemeInitializedEvent> {

    private final ReactiveCustomClient reactiveCustomClient;

    public FileInitializer(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    @Override
    public void onApplicationEvent(@NonNull SchemeInitializedEvent event) {
        initFilePolicyLocal();
    }

    private void initFilePolicyLocal() {
        // init file policy LOCAL
        Optional<List<FilePolicy>> filePoliciesOptional
            = reactiveCustomClient.findAll(FilePolicy.class,
                filePolicy -> FileConst.POLICY_LOCAL.equals(filePolicy.getName()))
            .collectList().blockOptional();
        if (filePoliciesOptional.isEmpty() || filePoliciesOptional.get().isEmpty()) {
            reactiveCustomClient
                .create(FilePolicy.builder().name(FileConst.POLICY_LOCAL).build())
                .block();
        }

        // init file setting policy
        Optional<List<FileSetting>> fileSettingsOptional =
            reactiveCustomClient.findAll(FileSetting.class, null)
                .collectList().blockOptional();
        if (fileSettingsOptional.isEmpty() || fileSettingsOptional.get().isEmpty()) {
            reactiveCustomClient
                .create(FileSetting.builder().policy(FileConst.POLICY_LOCAL).build())
                .block();
        }
    }
}
